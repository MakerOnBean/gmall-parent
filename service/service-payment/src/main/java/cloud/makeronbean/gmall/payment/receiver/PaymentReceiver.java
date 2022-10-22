package cloud.makeronbean.gmall.payment.receiver;

import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.payment.service.PaymentInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author makeronbean
 */
@Component
public class PaymentReceiver {

    @Autowired
    private PaymentInfoService paymentInfoService;


    /**
     * 关闭支付状态
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_CLOSE,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,autoDelete = "false"),
            key = MqConst.ROUTING_PAYMENT_CLOSE
    ))
    public void closePaymentStatus(Long orderId, Message message, Channel channel) {

        try {
            paymentInfoService.closePaymentStatus(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
