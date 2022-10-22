package cloud.makeronbean.gmall.list.receiver;

import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author makeronbean
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 监听商品上架
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS,durable = "true",autoDelete = "false"),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upper(Long skuId,Message message, Channel channel){
        try {
            searchService.upperGoods(skuId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            // 如果有问题，记录日志、记录数据库、发送程序员短信等

            e.printStackTrace();
        }
    }

    /**
     * 监听商品下架
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS,durable = "true",autoDelete = "false"),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lower(Long skuId,Message message,Channel channel) {
        try {
            searchService.lowerGoods(skuId);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            // 如果有问题，记录日志、记录数据库、发送程序员短信等

            e.printStackTrace();
        }
    }
}
