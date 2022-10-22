package cloud.makeronbean.gmall.order.receiver;

import cloud.makeronbean.gmall.client.PaymentFeignClient;
import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.enums.OrderStatus;
import cloud.makeronbean.gmall.model.enums.PaymentStatus;
import cloud.makeronbean.gmall.model.enums.ProcessStatus;
import cloud.makeronbean.gmall.model.order.OrderInfo;
import cloud.makeronbean.gmall.model.payment.PaymentInfo;
import cloud.makeronbean.gmall.order.mapper.OrderInfoMapper;
import cloud.makeronbean.gmall.order.service.OrderService;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;


/**
 * @author makeronbean
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    /**
     * 监听库存系统的消息
     * 用于确定是否该有库存
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = MqConst.ROUTING_WARE_ORDER
    ))
    public void stockOrderStatus(String strJson, Message message, Channel channel) {
        try {
            if (!StringUtils.isEmpty(strJson)) {
                Map<String, String> resultMap = JSONObject.parseObject(strJson, Map.class);
                String orderId = resultMap.get("orderId");
                String status = resultMap.get("status");
                if ("DEDUCTED".equals(status)) {
                    orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                } else {
                    orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                    // 库存不足，介入客服等
                }
            }
        } catch (NumberFormatException e) {
            // 抛出异常，短信通知程序员
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    /**
     * 修改订单状态为支付完成状态
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = MqConst.ROUTING_PAYMENT_PAY
    ))
    public void updateOrderStatusToPaid(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                // 修改状态
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                // 发送消息 扣减库存
                orderService.sendOrderStatus(orderId);
            }
        } catch (Exception e) {
            // 记录日志，短信通知等
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    /**
     * 订单延迟消息监听器
     * 用于过期的关闭未支付的订单
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrderStatus(Long orderId, Message message, Channel channel) {
        try {
            if (orderId != null) {
                OrderInfo orderInfo = orderService.getOrderInfo(orderId);
                if (orderInfo != null && OrderStatus.UNPAID.name().equals(orderInfo.getOrderStatus()) && ProcessStatus.UNPAID.name().equals(orderInfo.getProcessStatus())) {
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    // 是否存在支付信息
                    if (paymentInfo != null && PaymentStatus.UNPAID.name().equals(paymentInfo.getPaymentStatus())) {
                        // 是否存在支付宝支付信息
                        Boolean flag = paymentFeignClient.checkPayment(orderId);
                        if (flag) {
                            // 关闭支付宝交易订单
                            Boolean flag2 = paymentFeignClient.closePay(orderId);
                            if (flag2) {

                                orderService.execExpiredOrder(orderId,"2");
                            } else {
                                // 可能是支付成功
                            }
                        } else {
                            orderService.execExpiredOrder(orderId,"2");
                        }

                    } else {
                        orderService.execExpiredOrder(orderId,"1");
                    }


                }
            }
        } catch (Exception e) {
            // 写入日志、数据库等

            e.printStackTrace();
        }
        // 手动发送 ack 确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
