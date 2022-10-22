package cloud.makeronbean.gmall.cart.service;


import cloud.makeronbean.gmall.model.cart.CartInfo;
import cloud.makeronbean.gmall.model.list.Goods;

import java.util.List;

/**
 * @author makeronbean
 */
public interface CartService {

    /**
     * 添加购物车
     */
    void addToCart(Long skuId, Integer skuNum, String userId);

    /**
     * 查询购物车中有哪些数据
     */
    List<CartInfo> cartList(String userId, String userTempId);

    /**
     * 修改购物车选中状态
     */
    void checkCart(String userId, Long skuId, Integer isChecked);

    /**
     * 删除购物车
     */
    void deleteCart(String userId, Long skuId);

    /**
     * 获取选中的购物车列表
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 全选或全不选购物车列表
     */
    void allCheckCart(String userId, Integer isChecked);

    /**
     * 清空购物车
     */
    void clearCart(String userId);
}
