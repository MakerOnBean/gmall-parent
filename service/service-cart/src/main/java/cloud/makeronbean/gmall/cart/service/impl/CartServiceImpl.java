package cloud.makeronbean.gmall.cart.service.impl;

import cloud.makeronbean.gmall.cart.service.CartService;
import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.util.DateUtil;
import cloud.makeronbean.gmall.model.cart.CartInfo;
import cloud.makeronbean.gmall.model.list.Goods;
import cloud.makeronbean.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author makeronbean
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 添加购物车
     *
     * redis中存储方式：
     *  key：用户id
     *  filed：商品id
     *  value：商品对象信息
     *
     * 思路：
     *  查询sku信息
     *  判断购物车中是否存在
     *      如果存在，数量+
     *      如果不存在，新建添加
     */
    @Override
    public void addToCart(Long skuId, Integer skuNum, String userId) {
        // 获取key
        String cartKey = this.getCartKey(userId);

        // 获取redis hash操作对象
        // String key ，String field , CartInfo value
        BoundHashOperations<String, String, CartInfo> hashOps = redisTemplate.boundHashOps(cartKey);

        // 购物车详情信息对象
        CartInfo cartInfo = null;

        // 判断该用户购物车是否存在此商品数据
        if (hashOps.hasKey(skuId.toString())) {
            // 购物车中存在该商品

            // 价格
            BigDecimal price = productFeignClient.getSkuPrice(skuId);

            cartInfo = hashOps.get(skuId.toString());
            cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
            cartInfo.setUpdateTime(new Date());
            cartInfo.setSkuPrice(price);


        } else {
            // 获取购物车所需数据
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

            // 新建购物车并添加
            cartInfo = new CartInfo();
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
            cartInfo.setSkuPrice(skuInfo.getPrice());
        }
        hashOps.put(skuId.toString(),cartInfo);
    }


    /**
     * 查询购物车中有哪些数据
     *  合并未登录状态和登陆状态的购物车
     */
    @Override
    public List<CartInfo> cartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = null;

        // 获取未登录的购物车信息
        List<CartInfo> noLoginCartInfoList = null;
        if (!StringUtils.isEmpty(userTempId)) {
            String cartTempKey = this.getCartKey(userTempId);
            noLoginCartInfoList = redisTemplate.boundHashOps(cartTempKey).values();
        }

        // 未登录
        if (StringUtils.isEmpty(userId)) {
            return noLoginCartInfoList;
        }

        // 获取登陆后的购物车信息
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> hashOps = redisTemplate.boundHashOps(cartKey);
        if (!CollectionUtils.isEmpty(noLoginCartInfoList)) {
            // 合并购物车
            noLoginCartInfoList.forEach(cartInfo -> {
                if (hashOps.hasKey(cartInfo.getSkuId().toString())) {
                    // 存在
                    CartInfo loginCartInfo = hashOps.get(cartInfo.getSkuId().toString());
                    // 更新数量
                    loginCartInfo.setSkuNum(loginCartInfo.getSkuNum() + cartInfo.getSkuNum());
                    // 更新修改时间
                    loginCartInfo.setUpdateTime(new Date());
                    // 更新选中状态
                    if (cartInfo.getIsChecked() == 1) {
                        loginCartInfo.setIsChecked(1);
                    }

                    hashOps.put(cartInfo.getSkuId().toString(),loginCartInfo);
                } else {
                    // 不存在
                    // 更新用户id
                    cartInfo.setUserId(userId);
                    // 更新修改时间
                    cartInfo.setUpdateTime(new Date());
                    hashOps.put(cartInfo.getSkuId().toString(),cartInfo);
                }
            });

            // 清除临时购物车数据
            redisTemplate.delete(this.getCartKey(userTempId));
        }

        cartInfoList = hashOps.values();

        // 当没有购物车数据时
        if (CollectionUtils.isEmpty(cartInfoList)) {
            cartInfoList = new ArrayList<>();
        }

        // 排序
        cartInfoList.sort((o1,o2) -> DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(),Calendar.SECOND));

        return cartInfoList;
    }


    /**
     * 修改购物车选中状态
     */
    @Override
    public void checkCart(String userId, Long skuId, Integer isChecked) {
        BoundHashOperations<String, String, CartInfo> hashOps = redisTemplate.boundHashOps(this.getCartKey(userId));
        if (hashOps.hasKey(skuId.toString())) {
            CartInfo cartInfo = hashOps.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            hashOps.put(skuId.toString(), cartInfo);
        }

    }


    /**
     * 删除购物车
     */
    @Override
    public void deleteCart(String userId, Long skuId) {
        BoundHashOperations<String,String,CartInfo> hashOps = redisTemplate.boundHashOps(this.getCartKey(userId));
        if (hashOps.hasKey(skuId.toString())) {
            hashOps.delete(skuId.toString());
        }
    }


    /**
     * 获取选中的购物车列表
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        BoundHashOperations<String, String, CartInfo> hashOps = redisTemplate.boundHashOps(this.getCartKey(userId));
        List<CartInfo> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)) {
            return values.stream().filter(item -> {
                // 将价格修改为实时价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(item.getSkuId());
                item.setSkuPrice(skuPrice);
                return item.getIsChecked() == 1;
            }).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    /**
     * 全选或全不选购物车列表
     */
    @Override
    public void allCheckCart(String userId, Integer isChecked) {
        // 查询该用户的购物车
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(this.getCartKey(userId)).values();

        // 遍历更改所有购物车项订单状态
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            cartInfoList.forEach(cartInfo -> {
                if (!cartInfo.getIsChecked().equals(isChecked)) {
                    this.checkCart(userId, cartInfo.getSkuId(), isChecked);
                }
            });
        }
    }


    /**
     * 清空购物车
     */
    @Override
    public void clearCart(String userId) {
        this.redisTemplate.delete(this.getCartKey(userId));
    }


    /**
     * 获取redis中存储的key
     * user:userId:cart
     */
    private String getCartKey(String userId) {
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
}
