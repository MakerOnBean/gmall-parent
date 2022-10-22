package cloud.makeronbean.gmall.activity.service.impl;

import cloud.makeronbean.gmall.activity.mapper.SeckillGoodsMapper;
import cloud.makeronbean.gmall.activity.service.SeckillGoodsService;
import cloud.makeronbean.gmall.activity.utils.CacheHelper;
import cloud.makeronbean.gmall.client.UserFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.result.ResultCodeEnum;
import cloud.makeronbean.gmall.common.util.DateUtil;
import cloud.makeronbean.gmall.common.util.MD5;
import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.activity.OrderRecode;
import cloud.makeronbean.gmall.model.activity.SeckillGoods;
import cloud.makeronbean.gmall.model.activity.UserRecode;
import cloud.makeronbean.gmall.model.order.OrderDetail;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.user.UserAddress;
import cloud.makeronbean.gmall.service.RabbitService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author makeronbean
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 查询今日可秒杀的所有商品
     */
    @Override
    public List<SeckillGoods> findAll() {
        return redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }


    /**
     * 根据skuId查询秒杀商品信息
     */
    @Override
    public SeckillGoods getSeckillGoods(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(String.valueOf(skuId));
    }


    /**
     * 生成抢单码
     */
    @Override
    public String getSeckillSkuIdStr(Long skuId, String userId) {
        Object seckillGoodsObj = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(String.valueOf(skuId));
        if (seckillGoodsObj != null) {
            SeckillGoods seckillGoods = (SeckillGoods) seckillGoodsObj;
            Date startTime = seckillGoods.getStartTime();
            Date endTime = seckillGoods.getEndTime();
            Date nowTime = new Date();
            if (DateUtil.dateCompare(startTime,nowTime) && DateUtil.dateCompare(nowTime,endTime)) {
                return MD5.encrypt(userId);
            }
        }
        return null;
    }


    /**
     * 秒杀下单
     */
    @Override
    public Result seckillOrder(Long skuId, String skuIdStr, String userId) {
        // 判断抢单码
        if (MD5.encrypt(String.valueOf(userId)).equals(skuIdStr)) {
            // 判断状态位
            String status = (String) CacheHelper.get(String.valueOf(skuId));
            if ("1".equals(status)) {
                // 封装抢单对象
                UserRecode userRecode = new UserRecode();
                userRecode.setUserId(userId);
                userRecode.setSkuId(skuId);

                // 发送消息
                rabbitService.send(MqConst.EXCHANGE_DIRECT_SECKILL_USER,
                        MqConst.ROUTING_SECKILL_USER,
                        userRecode);

                return Result.ok();
            } else if ("0".equals(status)){
                return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
            } else {
                return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
            }

        } else {
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
    }


    /**
     * 处理秒杀下单的消息
     * 实现秒杀下单
     */
    @Override
    public void seckillUser(String userId, Long skuId) {
        // 校验状态位
        String status = (String) CacheHelper.get(String.valueOf(skuId));
        if ("0".equals(status)) {
            return;
        }

        // 判断用户是否下单
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER+userId,String.valueOf(skuId),RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!flag) {
            return;
        }

        // 获取队列中的商品
        Object skuIdStr = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(skuIdStr)) {
            redisTemplate.convertAndSend(RedisConst.SECKILL_TOPIC,skuId+":0");
            return;
        }

        // 订单数据存入Redis
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));
        orderRecode.setNum(1);
        orderRecode.setSeckillGoods(this.getSeckillGoods(skuId));
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(userId,orderRecode);

        // 更新库存
        this.updateStockCount(skuId);
    }


    /**
     * 页面定时任务
     * 查询秒杀是否成功
     */
    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 查询用户是否有抢单标示
        Boolean flag = redisTemplate.hasKey(RedisConst.SECKILL_USER+userId);
        if (flag) {
            // 查询是否存在临时订单数据
            OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
            if (orderRecode != null) {
                // 抢单成功但未支付
                return Result.build(orderRecode,ResultCodeEnum.SECKILL_SUCCESS);
            }

            // 查询总订单，如果下单了数据就在这里
            // 总订单 redis中的存储结构   SECKILL_ORDERS_USERS  userId  orderId
            Boolean flag2 = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
            if (flag2) {
                // 下单成功
                return Result.build( orderRecode,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
            }

        }

        // 判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)) {
            // 已经售罄
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }

        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }


    /**
     * 查询订单详情、送货地址、总金额
     * 用于返回给trade页面
     */
    @Override
    public Result trade(String userId) {
        // 查询临时订单
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);

        // 判断是否存在
        if (orderRecode == null) {
            return Result.fail().message("非法请求");
        }

        // 封装订单详情
        List<OrderDetail> orderDetailList = new ArrayList<>();
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetail.setSkuNum(1);
        orderDetailList.add(orderDetail);

        // 地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressByUserId(Long.parseLong(userId));

        // 总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        Map<String,Object> map = new HashMap<>();
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",orderDetailList);
        map.put("totalAmount",orderInfo.getTotalAmount());
        map.put("totalNum",1);

        return Result.ok(map);

    }


    /**
     * 根据skuId更新秒杀商品的库存
     */
    private void updateStockCount(Long skuId) {
        Lock lock = new ReentrantLock();
        lock.lock();

        try {
            // 获取剩余数量
            Long stockNum = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
            // 更新mysql中的数据
            SeckillGoods seckillGoods = this.getSeckillGoods(skuId);
            seckillGoods.setStockCount(stockNum.intValue());
            seckillGoodsMapper.updateById(seckillGoods);

            // 更新redis中的数据
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(String.valueOf(skuId),seckillGoods);
        } finally {
            lock.unlock();
        }

    }
}
