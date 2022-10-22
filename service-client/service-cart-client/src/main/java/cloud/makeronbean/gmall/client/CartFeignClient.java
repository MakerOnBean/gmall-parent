package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.CartDegradeFeignClientImpl;
import cloud.makeronbean.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author makeronbean
 */
@Component
@FeignClient(value = "service-cart",fallback = CartDegradeFeignClientImpl.class)
public interface CartFeignClient {

    /**
     * 获取选中的购物车列表
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable String userId);

}
