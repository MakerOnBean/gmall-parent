package cloud.makeronbean.gmall.client.impl;

import cloud.makeronbean.gmall.client.PaymentFeignClient;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import org.springframework.stereotype.Component;

/**
 * @author makeronbean
 */
@Component
public class PaymentDegradeFeignClientImpl implements PaymentFeignClient {
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo) {
        return null;
    }

    @Override
    public Boolean closePay(Long orderId) {
        return null;
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        return null;
    }
}
