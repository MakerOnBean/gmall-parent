package cloud.makeronbean.gmall.cart.controller;

import cloud.makeronbean.gmall.cart.service.CartService;
import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.AuthContextHolder;
import cloud.makeronbean.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;


    /**
     * 清空购物车
     */
    @GetMapping("/clearCart")
    public Result clearCart(HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.clearCart(userId);
        return Result.ok();
    }


    /**
     * 全选或全不选购物车列表
     */
    @GetMapping("/allCheckCart/{isChecked}")
    public Result allCheckCart(@PathVariable Integer isChecked,
                               HttpServletRequest request) {
        // 获取userId或userTempId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.allCheckCart(userId,isChecked);
        return Result.ok();

    }


    /**
     * 获取选中的购物车列表
     */
    @GetMapping("/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }


    /**
     * 删除购物车
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(userId, skuId);
        return Result.ok();

    }


    /**
     * 修改购物车选中状态
     */
    @GetMapping("/checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId,skuId,isChecked);
        return Result.ok();
    }


    /**
     * 查询购物车中有哪些数据
     */
    @GetMapping("/cartList")
    public Result cartList(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        String userTempId = AuthContextHolder.getUserTempId(request);
        List<CartInfo> cartInfoList = cartService.cartList(userId,userTempId);
        return Result.ok(cartInfoList);
    }


    /**
     * 添加购物车
     */
    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {

        // 获取userId
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }

        cartService.addToCart(skuId,skuNum,userId);
        return Result.ok();
    }

}
