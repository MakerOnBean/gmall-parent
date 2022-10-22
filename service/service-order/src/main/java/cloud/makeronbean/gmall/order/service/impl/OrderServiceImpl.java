package cloud.makeronbean.gmall.order.service.impl;

import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.util.HttpClientUtil;
import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.enums.OrderStatus;
import cloud.makeronbean.gmall.model.enums.PaymentWay;
import cloud.makeronbean.gmall.model.enums.ProcessStatus;
import cloud.makeronbean.gmall.model.order.OrderDetail;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.order.mapper.OrderDetailMapper;
import cloud.makeronbean.gmall.order.mapper.OrderInfoMapper;
import cloud.makeronbean.gmall.order.service.OrderService;
import cloud.makeronbean.gmall.service.RabbitService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private ExecutorService executorService;

    @Value("${ware.url}")
    private String url;

    /**
     * 下订单
     * <br/>
     * 涉及的表<br/>
     * order_info<br/>
     * order_detail
     * <br/>
     * 关系：一对多
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(OrderInfo orderInfo) {
        // total_amount 总金额
        orderInfo.sumTotalAmount();
        // order_status 订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // payment_way 支付方式
        orderInfo.setPaymentWay(PaymentWay.ONLINE.name());
        // out_trade_no 第三方支付 订单交易编号
        orderInfo.setOutTradeNo("makeronbean" + UUID.randomUUID().toString().replaceAll("-",""));
        // trade_body 第三方支付 订单说明
        StringBuilder builder = new StringBuilder();
        orderInfo.getOrderDetailList().forEach(orderDetail -> {
            builder.append(orderDetail.getSkuName()).append("    ");
        });
        String tradeBody = builder.toString();
        if (tradeBody.length() > 100) {
            tradeBody = tradeBody.substring(0, 100);
        }
        orderInfo.setTradeBody(tradeBody);
        // operate_time 操作时间
        orderInfo.setOperateTime(new Date());
        // expire_time 失效时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // process_status 进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());


        this.saveOrderInfo(orderInfo);

        // 删除购物车项
        //redisTemplate.delete(RedisConst.USER_KEY_PREFIX + orderInfo.getUserId() + RedisConst.USER_CART_KEY_SUFFIX);

        // 向MQ中发送两小时后的延时消息，用于关闭两小时后未支付的订单
        // 遗留的问题：延迟插件执行过慢，暂时使用线程池解决
        executorService.execute(() -> {
            rabbitService.sendDelay(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                    MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),
                    MqConst.DELAY_TIME);
        });


        // 返回订单id
        return orderInfo.getId();
    }


    /**
     * 生成流水号，并存储到redis中
     */
    @Override
    public String getTradeNo(String userId) {
        // 生成流水号
        String tradeNo = UUID.randomUUID().toString().replaceAll("-","");
        // 存储到redis
        String tradeKey = this.getTradeKey(userId);
        redisTemplate.opsForValue().set(tradeKey,tradeNo,30, TimeUnit.MINUTES);
        //返回给页面
        return tradeNo;
    }


    /**
     * 校验流水号
     */
    @Override
    public boolean checkTradeNo(String userId, String tradeNoCode) {
        if (StringUtils.isEmpty(tradeNoCode)) {
            return false;
        }
        String tradeKey = this.getTradeKey(userId);
        String tradeNoFromRedis = redisTemplate.opsForValue().get(tradeKey);
        return tradeNoCode.equals(tradeNoFromRedis);
    }


    /**
     * 删除流水号
     */
    @Override
    public void deleteTradeNo(String userId) {
        String tradeKey = this.getTradeKey(userId);
        redisTemplate.delete(tradeKey);
    }


    /**
     * 验证库存
     */
    @Override
    public boolean checkStock(String skuId, String skuNum) {
        String result = HttpClientUtil.doGet(url + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }


    /**
     * 订单分页查询
     */
    @Override
    public IPage<OrderInfo> getOrderPage(Page<OrderInfo> page, Integer pageNum, Integer limit, Long userId, String orderStatus) {
        IPage<OrderInfo> orderPage = orderInfoMapper.getOrderPage(page, userId, orderStatus);
        List<OrderInfo> orderInfoList = orderPage.getRecords();
        if (!CollectionUtils.isEmpty(orderInfoList)) {
            orderInfoList.forEach(item -> {
                item.setOrderStatusName(OrderStatus.getStatusNameByStatus(item.getOrderStatus()));
            });
        }
        return orderPage;
    }


    /**
     * 处理过期订单
     */
    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        // 关闭过期未支付订单
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);

        // 判断是否需要关闭支付信息
        if ("2".equals(flag)) {
            rabbitService.send(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,
                    MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }


    /**
     * 根据订单Id 修改订单的状态
     */
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        baseMapper.updateById(orderInfo);
    }


    /**
     * 根据订单Id查询订单
     * 携带订单明细
     */
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.getOrderInfo(orderId);
        return orderInfo;
    }


    /**
     * 发送消息给第三方库存，扣减库存
     */
    @Override
    public void sendOrderStatus(Long orderId) {
        // 修改订单状态
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        // 查询订单
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        // 封装数据
        String strJson = this.initWareOrder(orderInfo);
        // 发送消息
        rabbitService.send(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,strJson);
    }


    /**
     * 构建用于返回给库存系统的map集合
     */
    @Override
    public Map<String, Object> getInitWareParamMap(OrderInfo orderInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId",orderInfo.getWareId());
        List<Map<String,Object>> details =
                orderInfo.getOrderDetailList().stream().map(detail -> {
                    Map<String,Object> detailMap = new HashMap<>(3);
                    detailMap.put("skuId",detail.getSkuId());
                    detailMap.put("skuNum",detail.getSkuNum());
                    detailMap.put("skuName",detail.getSkuName());
                    return detailMap;
                }).collect(Collectors.toList());
        map.put("details",details);
        return map;
    }


    /**
     * 拆单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderInfo> orderSplit(String orderIdStr, String wareSkuListStr) {

        // 获取需要拆单的订单id
        Long orderId = Long.valueOf(orderIdStr);
        // 获取仓库与sku对应的关系的集合
        List<Map> arrayMap = JSON.parseArray(wareSkuListStr, Map.class);
        // 创建一个用户返回的集合
        List<OrderInfo> resultList = new ArrayList<>(arrayMap.size());
        // 获取需要拆单的订单对象
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        // 遍历集合，根据仓库进行拆单
        arrayMap.forEach(item -> {
            // 获取仓库id
            String wareId = (String) item.get("wareId");
            // 获取该仓库下的skuId
            List<String> skuIds = (List) item.get("skuIds");
            // 创建子订单对象
            OrderInfo subOrderInfo = new OrderInfo();
            // 拷贝父订单中的公有数据到子订单
            BeanUtils.copyProperties(orderInfo,subOrderInfo);
            // 设置子订单中的其他非公有数据
            // id
            subOrderInfo.setId(null);
            // parent_order_id
            subOrderInfo.setParentOrderId(orderId);
            // wareId
            subOrderInfo.setWareId(wareId);

            // 总金额--需要先设置订单详情到订单中
            // 获取父订单的所有订单明细
            List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)) {
                // 用户拼接商品描述
                StringBuilder sb = new StringBuilder();

                // 收集该仓库下的子订单明细
                List<OrderDetail> subOrderDetailList =
                orderDetailList.stream().filter(orderDetail -> {
                    for (String skuId : skuIds) {
                        if (Long.valueOf(skuId).equals(orderDetail.getSkuId())){
                            sb.append(orderDetail.getSkuName()).append("  ");
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
                // 将收集的 子订单详情信息集合 放入子订单信息对象中
                subOrderInfo.setOrderDetailList(subOrderDetailList);
                // 计算并回填总金额属性
                subOrderInfo.sumTotalAmount();

                // order_comment
                subOrderInfo.setOrderComment(sb.toString());
            }

            // 添加子订单信息和子订单详情信息到数据库中
            saveOrderInfo(subOrderInfo);

            // 收集每个仓库下的订单信息
            resultList.add(subOrderInfo);
        });

        // 修改父订单状态
        this.updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return resultList;
    }


    /**
     * 保存订单信息和订单详情信息到数据库中
     */
    private void saveOrderInfo(OrderInfo orderInfo) {
        // 保存订单
        orderInfoMapper.insert(orderInfo);

        // 保存订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            orderDetailList.forEach(orderDetail -> {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            });
        }
    }


    /**
     * 封装扣减库存系统的参数
     */
    private String initWareOrder(OrderInfo orderInfo) {
        Map<String, Object> map = getInitWareParamMap(orderInfo);
        return JSON.toJSONString(map);
    }





    /**
     * 根据id生成流水号在redis中的key
     */
    private String getTradeKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.TRADE_CODE;
    }
}
