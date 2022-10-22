package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.ItemDegradeFeignClientImpl;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author makeronbean
 */
@FeignClient(value = "service-item",fallback = ItemDegradeFeignClientImpl.class)
@Component
public interface ItemFeignClient {

    /**
     * 获取商品详细数据
     */
    @GetMapping("/api/item/{skuId}")
    Result<Map<String,Object>> getItem(@PathVariable Long skuId);

}
