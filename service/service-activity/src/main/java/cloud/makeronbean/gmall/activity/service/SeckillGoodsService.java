package cloud.makeronbean.gmall.activity.service;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author makeronbean
 */
public interface SeckillGoodsService {
    /**
     * 查询今日可秒杀的所有商品
     */
    List<SeckillGoods> findAll();

    /**
     * 根据skuId查询秒杀商品信息
     */
    SeckillGoods getSeckillGoods(Long skuId);

    /**
     * 生成抢单码
     */
    String getSeckillSkuIdStr(Long skuId, String userId);

    /**
     * 秒杀下单
     */
    Result seckillOrder(Long skuId, String skuIdStr, String userId);

    /**
     * 处理秒杀下单的消息
     * 实现秒杀下单
     */
    void seckillUser(String userId, Long skuId);


    /**
     * 页面定时任务
     * 查询秒杀是否成功
     */
    Result checkOrder(Long skuId, String userId);

    /**
     * 查询订单详情、送货地址、总金额
     * 用于返回给trade页面
     */
    Result trade(String userId);
}
