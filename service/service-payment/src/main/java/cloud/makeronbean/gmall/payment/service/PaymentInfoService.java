package cloud.makeronbean.gmall.payment.service;

import cloud.makeronbean.gmall.model.enums.PaymentStatus;
import cloud.makeronbean.gmall.model.enums.PaymentType;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author makeronbean
 */
public interface PaymentInfoService {
    /**
     * 保存支付信息
     */
    void savePaymentInfo(OrderInfo orderInfo, PaymentType paymentType);


    /**
     * 根据第三方交易号和支付方式查询支付信息
     * 用于第二次验签
     */
    PaymentInfo getPaymentInfo(String outTradeNo, PaymentType paymentType);


    /**
     * 支付宝回调 修改支付状态
     * 将未支付信息修改为已支付
     */
    void updatePaymentInfo(Long paymentId, Map<String, String> paramsMap);

    /**
     * 通过orderId查询支付信息表
     */
    PaymentInfo getByOrderId(Long orderId, PaymentType paymentType);

    /**
     * 修改订单状态
     */
    void updatePaymentStatus(PaymentInfo paymentInfo, PaymentStatus paymentStatus);

    /**
     * 关闭支付信息
     */
    void closePaymentStatus(Long orderId);
}
