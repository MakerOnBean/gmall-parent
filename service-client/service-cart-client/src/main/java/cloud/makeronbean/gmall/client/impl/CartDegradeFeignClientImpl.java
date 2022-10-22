package cloud.makeronbean.gmall.client.impl;


import cloud.makeronbean.gmall.client.CartFeignClient;
import cloud.makeronbean.gmall.model.cart.CartInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author makeronbean
 */
@Component
public class CartDegradeFeignClientImpl implements CartFeignClient {
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        return null;
    }
}
