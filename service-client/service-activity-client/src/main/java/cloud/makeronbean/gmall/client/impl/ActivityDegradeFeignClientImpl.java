package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.ActivityFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author makeronbean
 */
@Component
public class ActivityDegradeFeignClientImpl implements ActivityFeignClient {
    @Override
    public Result trade() {
        return Result.fail().message("数据获取失败");
    }

    @Override
    public Result getSeckillGoods(Long skuId) {
        return Result.fail();
    }

    @Override
    public Result findAll() {
        return Result.fail();
    }
}
