package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.ActivityFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;


/**
 * @author makeronbean
 */
@Controller
public class SeckillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    /**
     * 去结算页
     */
    @GetMapping("/seckill/trade.html")
    public String trade(Model model) {
        Result result = activityFeignClient.trade();
        if (result.isOk()) {
            Map resultMap = (Map) result.getData();
            model.addAllAttributes(resultMap);
            return "seckill/trade";
        }
        model.addAttribute("message",result.getMessage());
        return "seckill/fail";
    }


    /**
     * 跳转到抢单页面
     */
    @GetMapping("/seckill/queue.html")
    public String queue(@RequestParam String skuId,
                        @RequestParam String skuIdStr,
                        Model model) {
        model.addAttribute("skuId",skuId);
        model.addAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }


    /**
     * 去秒杀首页
     */
    @GetMapping("/seckill.html")
    public String index(Model model) {
        Result result = activityFeignClient.findAll();
        Object data = result.getData();
        if (data != null) {
            model.addAttribute("list", data);
        }
        return "seckill/index";
    }


    /**
     * 去秒杀商品详情页
     */
    @GetMapping("/seckill/{skuId}.html")
    public String item(@PathVariable Long skuId, Model model) {
        Result result = activityFeignClient.getSeckillGoods(skuId);
        Object data = result.getData();
        if (data != null) {
            model.addAttribute("item",data);
        }
        return "seckill/item";
    }
}
