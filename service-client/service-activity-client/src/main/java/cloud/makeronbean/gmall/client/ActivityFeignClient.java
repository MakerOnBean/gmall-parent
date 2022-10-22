package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.ActivityDegradeFeignClientImpl;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

/**
 * @author makeronbean
 */
@Component
@FeignClient(value = "service-activity", fallback = ActivityDegradeFeignClientImpl.class)
public interface ActivityFeignClient {

    /**
     * 查询订单详情、送货地址、总金额
     * 用于返回给trade页面
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    Result trade();

    /**
     * 根据skuId查询秒杀商品信息
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    /**
     * 查询今日可秒杀的所有商品
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();
}
