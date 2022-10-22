package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.ProductFeignClient;
import cloud.makeronbean.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author makeronbean
 */
@Controller
public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;


    /**
     * 跳转到添加商品进购物车成功页
     */
    @GetMapping("/addCart.html")
    public String addCart(Long skuId, Integer skuNum, Model model){
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        model.addAttribute("skuInfo",skuInfo);
        model.addAttribute("skuNum",skuNum);
        return "cart/addCart";
    }


    /**
     * 跳转到购物车主页
     */
    @GetMapping("/cart.html")
    public String index(){
        return "cart/index";
    }

}
