package cloud.makeronbean.gmall.activity.controller;

import cloud.makeronbean.gmall.activity.service.SeckillGoodsService;
import cloud.makeronbean.gmall.client.OrderFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.AuthContextHolder;
import cloud.makeronbean.gmall.model.activity.SeckillGoods;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 保存下单数据
     */
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,
                              HttpServletRequest request) {
        // 获取用户id并设置到userInfo
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        Long orderId = orderFeignClient.submitOrderSeckill(orderInfo);
        if (orderId == null) {
            return Result.fail().message("下单失败");
        }

        // 处理缓存中的订单
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);

        // 缓存中添加总订单记录
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId);
        return Result.ok(orderId);
    }


    /**
     * 查询订单详情、送货地址、总金额
     * 用于返回给trade页面
     */
    @GetMapping("/auth/trade")
    public Result trade(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.trade(userId);
    }


    /**
     * 页面定时任务
     * 查询秒杀是否成功
     */
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId, HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);

        return seckillGoodsService.checkOrder(skuId,userId);

    }


    /**
     * 秒杀下单
     */
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,
                               @RequestParam String skuIdStr,
                               HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.seckillOrder(skuId,skuIdStr,userId);
    }


    /**
     * 生成抢单码
     */
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId,
                                     HttpServletRequest request) {

        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);

        String skuIdStr = seckillGoodsService.getSeckillSkuIdStr(skuId, userId);

        if (StringUtils.isEmpty(skuIdStr)) {
            return Result.fail().message("生成抢购码失败");
        }

        return Result.ok(skuIdStr);

    }


    /**
     * 根据skuId查询秒杀商品信息
     */
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId) {
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);
        return Result.ok(seckillGoods);
    }


    /**
     * 查询今日可秒杀的所有商品
     */
    @GetMapping("/findAll")
    public Result findAll() {
        List<SeckillGoods> seckillGoodsList = seckillGoodsService.findAll();
        return Result.ok(seckillGoodsList);
    }

}
