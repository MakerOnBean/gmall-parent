package cloud.makeronbean.gmall.payment.service.impl;

import cloud.makeronbean.gmall.client.OrderFeignClient;
import cloud.makeronbean.gmall.common.util.HttpClientUtil;
import cloud.makeronbean.gmall.model.enums.OrderStatus;
import cloud.makeronbean.gmall.model.enums.PaymentStatus;
import cloud.makeronbean.gmall.model.enums.PaymentType;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import cloud.makeronbean.gmall.payment.prop.AliPayProperties;
import cloud.makeronbean.gmall.payment.service.AlipayService;
import cloud.makeronbean.gmall.payment.service.PaymentInfoService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.http.HttpUtils;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import lombok.SneakyThrows;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author makeronbean
 */
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AliPayProperties aliPayProperties;
    /**
     * 支付宝下单提交
     * 需要在支付信息表中保存数据
     * 然后对接支付宝接口
     */
    @Override
    public String submitOrder(Long orderId) {
        // 查询订单
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        if (orderInfo == null || !OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus())) {
            return "当前订单状态异常";
        }

        // 在支付信息表中保存数据
        paymentInfoService.savePaymentInfo(orderInfo, PaymentType.ALIPAY);

        // 对接支付宝

        // 创建API对应的request 移动端需要使用另一个对象
        //AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest();
        // 移动端使用的对象
        AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();


        // 设置同步回调地址
        alipayRequest.setReturnUrl(aliPayProperties.getReturnPaymentUrl());
        // 在公共参数中设置回跳和通知地址
        // 设置异步回调地址
        alipayRequest.setNotifyUrl(aliPayProperties.getNotifyPaymentUrl());


        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        // 商品价格在这里写死
        bizContent.put("total_amount", 0.01);
        // 商品名称
        bizContent.put("subject", orderInfo.getTradeBody());
        // 销售产品码，写死即可，移动端使用另一个参数
        //bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("product_code", "QUICK_WAP_WAY");
        // 设置超时时间 - 相对时间
        //bizContent.put("timeout_expire", "10m");

        // 设置超时时间 - 绝对时间
        bizContent.put("time_expire", this.getTimeOut());

        // 填充业务参数
        alipayRequest.setBizContent(bizContent.toJSONString());
        String form= "" ;
        try  {
            // 调用SDK生成表单
            form = alipayClient.pageExecute(alipayRequest).getBody();
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }

        return form;
    }


    /**
     * 退款
     */
    @Override
    public boolean refund(Long orderId) {
        // 根据 orderId 查询支付信息
        PaymentInfo paymentInfo = paymentInfoService.getByOrderId(orderId,PaymentType.ALIPAY);

        // 判断
        if (paymentInfo == null || !"PAID".equals(paymentInfo.getPaymentStatus())) {
            return false;
        }

        // 退款申请
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("trade_no", paymentInfo.getTradeNo());
        bizContent.put("refund_amount", 0.01);
        bizContent.put("refund_result", "不想要了");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 修改订单记录表
            paymentInfoService.updatePaymentStatus(paymentInfo, PaymentStatus.CLOSED);
            // 修改订单记录

            return true;
        } else {
            return false;
        }

    }


    /**
     * 查询支付宝交易记录状态
     */
    @SneakyThrows
    @Override
    public Boolean checkPayment(Long orderId) {
        // 查询 PaymentInfo 对象
        PaymentInfo paymentInfo = paymentInfoService.getByOrderId(orderId, PaymentType.ALIPAY);

        // 请求支付宝接口，判断是否在支付宝生成了交易订单
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        return response.isSuccess();
    }


    /**
     * 支付宝关闭交易
     */
    @SneakyThrows
    @Override
    public Boolean closePay(Long orderId) {
        // 查询支付信息
        PaymentInfo paymentInfo = paymentInfoService.getByOrderId(orderId, PaymentType.ALIPAY);
        // 请求支付宝
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 获取超时时间
     */
    private String getTimeOut() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,10);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
    }
}
