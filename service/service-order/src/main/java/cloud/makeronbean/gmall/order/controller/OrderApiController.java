package cloud.makeronbean.gmall.order.controller;

import cloud.makeronbean.gmall.client.CartFeignClient;
import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.client.UserFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.AuthContextHolder;
import cloud.makeronbean.gmall.model.cart.CartInfo;
import cloud.makeronbean.gmall.model.order.OrderDetail;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.user.UserAddress;
import cloud.makeronbean.gmall.order.service.OrderService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ExecutorService executor;

    /**
     * /api/order/auth/{orderId}
     * 移动端订单详情
     */
    @GetMapping("/auth/{orderId}")
    public Result getOrderInfoForAndroid(@PathVariable Long orderId) {
        OrderInfo orderInfo = this.orderService.getOrderInfo(orderId);
        return Result.ok(orderInfo);
    }


    /**
     * 拆单
     * 由库存系统调用，参数为拆单格式
     *      例如：[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
     * 返回值为拆单后，根据仓库id分类的List<Map>，以json格式返回
     */
    @PostMapping("/orderSplit")
    public String orderSplit(String orderId, String wareSkuMap) {
        // 拆单
        List<OrderInfo> orderInfoList = orderService.orderSplit(orderId,wareSkuMap);

        // 返回数据
        List<Map<String, Object>> resultList = orderInfoList.stream().map(orderInfo ->
            orderService.getInitWareParamMap(orderInfo)
        ).collect(Collectors.toList());
        return JSON.toJSONString(resultList);
    }


    /**
     * 根据订单Id查询订单
     */
    @GetMapping("/inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }


    /**
     * 进入我的订单
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result getOrderPage(@PathVariable("page") Integer pageNum,
                               @PathVariable Integer limit,
                               HttpServletRequest request){
        String orderStatus = request.getParameter("orderStatus");
        Page<OrderInfo> page = new Page<>(pageNum,limit);
        String userId = AuthContextHolder.getUserId(request);
        IPage<OrderInfo> orderInfoPage = orderService.getOrderPage(page,pageNum,limit,Long.valueOf(userId),orderStatus);
        return Result.ok(orderInfoPage);
    }


    /**
     * 秒杀商品的下单
     */
    @PostMapping("/auth/submitOrderSeckill")
    public Long submitOrderSeckill(@RequestBody OrderInfo orderInfo) {

        Long orderId = orderService.submitOrder(orderInfo);
        // 返回订单id
        return orderId;
    }


    /**
     * 下订单
     * 优化后，使用异步对象实现
     */
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,
                              @RequestParam("tradeNo") String tradeNo,
                              HttpServletRequest request){
        // 获取用户id
        String userId = AuthContextHolder.getUserId(request);

        // 验证流水号
        boolean flag = orderService.checkTradeNo(userId, tradeNo);
        if (!flag) {
            return Result.fail().message("不能重复提交");
        }

        orderInfo.setUserId(Long.parseLong(userId));
        // 存储订单详情和订单明细到数据库
        Long orderId = orderService.submitOrder(orderInfo);

        // 删除流水号
        orderService.deleteTradeNo(userId);

        // 验证价格或库存不匹配时，会将提示信息放入该集合
        List<String> errorList = Collections.synchronizedList(new ArrayList<>());

        // 用于存放所有异步编排对象，最后需要等待所有对象执行完毕后才可以下一步操作
        List<CompletableFuture<Void>> futureList = Collections.synchronizedList(new ArrayList<>());
        // 验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            // 遍历订单明细，校验每个商品的库存和价格
            for (OrderDetail orderDetail : orderDetailList) {
                // 校验库存
                CompletableFuture<Void> stockFuture = CompletableFuture.runAsync(() -> {
                    if (!orderService.checkStock(String.valueOf(orderDetail.getSkuId()),
                            String.valueOf(orderDetail.getSkuNum()))) {
                        errorList.add(orderDetail.getSkuName() + "库存不足");
                    }
                }, executor);
                futureList.add(stockFuture);

                // 校验价格
                CompletableFuture<Void> priceFuture = CompletableFuture.runAsync(() -> {
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        // 设置新的价格
                        orderDetail.setOrderPrice(skuPrice);

                        // 修改购物车中的价格
                        // 提交订单失败返回后购物车将会变为最新价格
                        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
                        if (!CollectionUtils.isEmpty(cartCheckedList)) {
                            cartCheckedList.forEach(item -> {
                                redisTemplate.boundHashOps(RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX).put(item.getSkuId().toString(), item);
                            });
                        }
                        errorList.add(orderDetail.getSkuName() + " 价格有变动");
                    }
                }, executor);
                futureList.add(priceFuture);
            }

            // 等待所有任务执行完毕后，再进行下一步判断
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
            if (errorList.size() > 0) {
                return Result.fail().message(StringUtils.join(errorList,","));
            }
        }

        // 返回订单id
        return Result.ok(orderId);

    }


    /**
     * 去结算
     */
    @GetMapping("/auth/trade")
    public Result trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            return Result.fail().message("用户信息出错");
        }

        // 获取用户地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressByUserId(Long.parseLong(userId));

        // 获取购物车项
        List<OrderDetail> orderDetailList = null;
        List<CartInfo> cartList = cartFeignClient.getCartCheckedList(userId);
        if (!CollectionUtils.isEmpty(cartList)) {
            orderDetailList = cartList.stream().map(item -> {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(item.getSkuId());
                orderDetail.setSkuName(item.getSkuName());
                orderDetail.setImgUrl(item.getImgUrl());
                orderDetail.setOrderPrice(productFeignClient.getSkuPrice(item.getSkuId()));
                orderDetail.setSkuNum(item.getSkuNum());

                return orderDetail;
            }).collect(Collectors.toList());
        }

        // 数量
        int skuNum = 0;
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            skuNum = orderDetailList.size();
        }

        // 总金额
        BigDecimal totalAmount = new BigDecimal("0");
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderDetailList(orderDetailList);
            orderInfo.sumTotalAmount();
            totalAmount = orderInfo.getTotalAmount();
        }

        // 生成订单流水号
        String tradeNo = orderService.getTradeNo(userId);

        // 封装返回结果
        Map<String,Object> resultMap = new HashMap<>(4);
        resultMap.put("userAddressList",userAddressList);
        resultMap.put("detailArrayList",orderDetailList);
        resultMap.put("totalAmount",totalAmount);
        resultMap.put("totalNum",skuNum);
        resultMap.put("tradeNo",tradeNo);

        return Result.ok(resultMap);
    }
}
