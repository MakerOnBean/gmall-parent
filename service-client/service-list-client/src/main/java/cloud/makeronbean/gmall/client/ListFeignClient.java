package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.ListDegradeFeignClientImpl;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author makeronbean
 */
@FeignClient(value = "service-list",fallback = ListDegradeFeignClientImpl.class)
@Component
public interface ListFeignClient {

    /**
     * 商品搜索
     */
    @PostMapping("/api/list")
    Result list(@RequestBody SearchParam searchParam);


    /**
     * 更改商品热度排名
     */
    @GetMapping("/api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable Long skuId);
}
