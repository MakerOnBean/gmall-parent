package cloud.makeronbean.gmall.payment.service.impl;

import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.enums.PaymentStatus;
import cloud.makeronbean.gmall.model.enums.PaymentType;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import cloud.makeronbean.gmall.payment.mapper.PaymentInfoMapper;
import cloud.makeronbean.gmall.payment.service.PaymentInfoService;
import cloud.makeronbean.gmall.service.RabbitService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Date;
import java.util.Map;

/**
 * @author makeronbean
 */
@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;





    /**
     * 保存支付信息
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, PaymentType paymentType) {
        // 先看看之前该订单有没有相同支付方式，如果已经生成了，则返回
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderId,orderInfo.getId());
        wrapper.eq(PaymentInfo::getPaymentType,paymentType.name());
        Integer count = paymentInfoMapper.selectCount(wrapper);
        if (count > 0) {return;}

        // 如果之前未生成记录，则添加记录
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType.name());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());

        // 向支付信息表中添加字段
        paymentInfoMapper.insert(paymentInfo);
    }


    /**
     * 根据第三方交易号和支付方式查询支付信息
     * 用于第二次验签
     */
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, PaymentType paymentType) {
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOutTradeNo,outTradeNo).eq(PaymentInfo::getPaymentType,paymentType.name());
        return paymentInfoMapper.selectOne(wrapper);
    }


    /**
     * 支付宝回调 修改支付状态
     * 将未支付信息修改为已支付
     */
    @Override
    public void updatePaymentInfo(Long paymentId, Map<String, String> paramsMap) {
        try {
            PaymentInfo paymentInfo = paymentInfoMapper.selectById(paymentId);
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(JSON.toJSONString(paramsMap));
            paymentInfo.setUpdateTime(new Date());
            paymentInfoMapper.updateById(paymentInfo);
            // 通过消息队列修改订单状态为支付完成状态
            /*
                 与课件不同，课件为了预防消息重试导致重新修改订单，在监听器做判断
                 这里直接将发送消息语句放到成功修改订单状态后才执行确保消息只发送一次
             */
            rabbitService.send(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                    MqConst.ROUTING_PAYMENT_PAY,
                    paymentInfo.getOrderId());
        } catch (Exception e) {
            // 如果上面报错，则删除redis中的展位的数据，让下一次回调能够继续执行
            e.printStackTrace();
            redisTemplate.delete(paramsMap.get(paramsMap.get("notify_id")));
        }

    }


    /**
     * 通过orderId查询支付信息表
     */
    @Override
    public PaymentInfo getByOrderId(Long orderId, PaymentType paymentType) {
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderId,orderId).eq(PaymentInfo::getPaymentType,paymentType.name());
        return paymentInfoMapper.selectOne(wrapper);
    }


    /**
     * 修改订单状态
     */
    @Override
    public void updatePaymentStatus(PaymentInfo paymentInfo, PaymentStatus paymentStatus) {
        paymentInfo.setPaymentStatus(paymentStatus.name());
        paymentInfoMapper.updateById(paymentInfo);
    }


    /**
     * 关闭支付信息
     */
    @Override
    public void closePaymentStatus(Long orderId) {
        // 构建修改条件
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderId,orderId).eq(PaymentInfo::getPaymentStatus,"UNPAID");

        // 构建修改内容
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());
        paymentInfoMapper.update(paymentInfo,wrapper);
    }
}
