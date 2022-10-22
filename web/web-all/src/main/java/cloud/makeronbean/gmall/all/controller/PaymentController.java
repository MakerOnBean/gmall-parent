package cloud.makeronbean.gmall.all.controller;

import cloud.makeronbean.gmall.client.OrderFeignClient;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author makeronbean
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 跳转到选择付款页面
     */
    @GetMapping("/pay.html")
    public String pay(@RequestParam Long orderId, Model model) {
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        if (orderInfo != null) {
            model.addAttribute("orderInfo",orderInfo);
        }

        return "payment/pay";
    }


    /**
     * 跳转到支付成功页面
     */
    @GetMapping("/pay/success.html")
    public String success() {
        return "payment/success";
    }
}
