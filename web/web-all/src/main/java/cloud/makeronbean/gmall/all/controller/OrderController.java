package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.OrderFeignClient;
import cloud.makeronbean.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author makeronbean
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 跳转到我的订单
     */
    @GetMapping("/myOrder.html")
    public String myOrder(){
        return "order/myOrder";
    }


    /**
     * 跳转下订单页面
     */
    @GetMapping("/trade.html")
    public String index(Model model, HttpServletRequest request){
        Result<Map<String,Object>> trade = orderFeignClient.trade();
        Map<String, Object> resultMap = trade.getData();
        model.addAllAttributes(resultMap);

        return "order/trade";
    }
}
