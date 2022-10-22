package cloud.makeronbean.gmall.payment.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.common.util.HttpClientUtil;
import cloud.makeronbean.gmall.model.enums.PaymentType;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import cloud.makeronbean.gmall.payment.prop.AliPayProperties;
import cloud.makeronbean.gmall.payment.service.AlipayService;
import cloud.makeronbean.gmall.payment.service.PaymentInfoService;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.internal.util.StringUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author makeronbean
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class PaymentApiController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private AliPayProperties aliPayProperties;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 根据outTradeId查询支付宝支付记录
     */
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        return paymentInfoService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY);
    }



    /**
     * 支付宝关闭交易
     */
    @GetMapping("/closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId) {
        return alipayService.closePay(orderId);
    }


    /**
     * 查询支付宝交易记录状态
     */
    @GetMapping("/checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId) {

        Boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }


    /**
     * 退款
     */
    @GetMapping("/refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId) {
        boolean flag = alipayService.refund(orderId);
        return Result.ok();
    }


    /**
     * 支付宝下单提交
     */
    @GetMapping("/submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId) {
        return alipayService.submitOrder(orderId);
    }


    /**
     * 支付成功后的接口调用
     * 重定向到网页中
     */
    @RequestMapping("/callback/return")
    public String callback() {
        return "redirect:" + aliPayProperties.getReturnOrderUrl();
    }


    /**
     * 支付宝异步回调方法
     */
    @SneakyThrows
    @PostMapping("/callback/notify")
    @ResponseBody
    public String notifyCallback(@RequestParam Map<String, String> paramsMap) {
        // 调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap,
                                                        aliPayProperties.getAlipayPublicKey(),
                                                        aliPayProperties.getCharset(),
                                                        aliPayProperties.getSignType());
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // out_trade_no 第三发订单交易号
            String outTradeNo = paramsMap.get("out_trade_no");
            // total_amount 总金额
            String totalAmount = paramsMap.get("total_amount");
            // 本次应用的id
            String appId = paramsMap.get("app_id");
            // 交易状态
            String tradeStatus = paramsMap.get("trade_status");
            // 通知校验ID，8次回调值都相同
            String notifyId = paramsMap.get("notify_id");
            // 如果不存在数据，则该次支付宝回调发送的数据无效
            if (StringUtils.isEmpty(outTradeNo) || StringUtils.isEmpty(totalAmount) || StringUtils.isEmpty(appId)) {
                return "failure";
            }

            // 根据 outTradeNo 和 paymentType 查询支付信息
            PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY);

            // 二次校验成功
            if (aliPayProperties.getAppId().equals(appId) ||
                    paymentInfo.getTotalAmount() != null ||
                    new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount)) != 0 ||
                    "UNPAID".equals(paymentInfo.getPaymentStatus())) {

                // 获取状态，判断是否是完成支付
                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    Boolean flag = redisTemplate.opsForValue().setIfAbsent(notifyId, notifyId, 1461, TimeUnit.MINUTES);
                    if (flag) {
                        // 设置支付状态信息
                        paymentInfoService.updatePaymentInfo(paymentInfo.getId(),paramsMap);

                    }
                    return "success";
                }


                return "success";
            }


        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }

        return "failure";
    }
}
