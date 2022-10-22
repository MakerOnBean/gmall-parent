package cloud.makeronbean.gmall.payment.service;

/**
 * @author makeronbean
 */
public interface AlipayService {
    /**
     * 支付宝下单提交
     */
    String submitOrder(Long orderId);

    /**
     * 退款
     */
    boolean refund(Long orderId);

    /**
     * 查询支付宝交易记录状态
     */
    Boolean checkPayment(Long orderId);

    /**
     * 支付宝关闭交易
     */
    Boolean closePay(Long orderId);
}
