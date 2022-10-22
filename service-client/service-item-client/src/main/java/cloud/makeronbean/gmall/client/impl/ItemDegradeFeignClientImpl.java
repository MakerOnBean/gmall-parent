package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.ItemFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.stereotype.Component;

/**
 * @author makeronbean
 */
@Component
public class ItemDegradeFeignClientImpl implements ItemFeignClient {


    @Override
    public Result getItem(Long skuId) {
        return Result.fail();
    }
}
