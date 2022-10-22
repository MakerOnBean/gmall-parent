package cloud.makeronbean.gmall.client;

import cloud.makeronbean.gmall.client.impl.PaymentDegradeFeignClientImpl;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author makeronbean
 */
@FeignClient(value = "service-payment",fallback = PaymentDegradeFeignClientImpl.class)
@Component
public interface PaymentFeignClient {

    /**
     * 根据outTradeId查询支付宝支付记录
     */
    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    @ResponseBody
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);


    /**
     * 支付宝关闭交易
     */
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    @ResponseBody
    Boolean closePay(@PathVariable Long orderId);


    /**
     * 查询支付宝交易记录状态
     */
    @GetMapping("/api/payment/alipay/checkPayment/{orderId}")
    @ResponseBody
    Boolean checkPayment(@PathVariable Long orderId);


}
