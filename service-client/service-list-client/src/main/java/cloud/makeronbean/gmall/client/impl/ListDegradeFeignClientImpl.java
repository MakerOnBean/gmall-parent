package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.ListFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.list.SearchParam;
import org.springframework.stereotype.Component;

/**
 * @author makeronbean
 */
@Component
public class ListDegradeFeignClientImpl implements ListFeignClient {


    @Override
    public Result list(SearchParam searchParam) {
        return Result.fail();
    }

    @Override
    public Result incrHotScore(Long skuId) {
        return Result.fail();
    }
}
