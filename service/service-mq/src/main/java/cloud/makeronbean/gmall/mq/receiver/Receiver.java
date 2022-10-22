package cloud.makeronbean.gmall.mq.receiver;

import cloud.makeronbean.gmall.mq.config.DeadLetterMqConfig;
import cloud.makeronbean.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author makeronbean
 */
@Component
@Slf4j
public class Receiver {

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 监听延迟队列中的消息（基于插件）
     * <br/>
     * 为了保证幂等性，让消息只能消费一次，所以需要引入redis进行判断
     * 让重试机制还是发送五次消息，但是只消费一次
     * <br/>
     *      redis value：0表示未被消费，1表示已经被消费
     * <br/>
     *  存在的问题：如果redis中已经存在了`delayed:`开头的msg，并且又来了一个消息与redis中的key相同，那么就会存在误判，不会消费消息，直接进行确认消费
     */
    @SneakyThrows
    @RabbitListener(queues = DelayedMqConfig.QUEUE_DELAY)
    public void processDelayed(String msg, Message message, Channel channel) {

        // 只有第一次到达的消息返回值为true
        Boolean result = redisTemplate.opsForValue().setIfAbsent("delayed:" + msg, "0", 10, TimeUnit.MINUTES);
        if (!result){

            String secondResult = redisTemplate.opsForValue().get("delayed:" + msg);
            if ("1".equals(secondResult)) {
                // 已经被消费
                // 返回ack
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            } else {
                // 上一条消息并未进行消费
                //消费消息
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                log.info("测试延时队列：收到消息时间：{}，消息：{}",sdf.format(new Date()),msg);

                // 将消息标记为已经消费
                redisTemplate.opsForValue().set("delayed:"+ msg, "1", 10 ,TimeUnit.MINUTES);

                // 返回ack
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            }

        } else {
            // 第一次到达的消息会执行这里

            //消费消息
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            log.info("测试延时队列：收到消息时间：{}，消息：{}",sdf.format(new Date()),msg);

            // 将消息标记为已经消费
            redisTemplate.opsForValue().set("delayed:"+ msg, "1", 10 ,TimeUnit.MINUTES);

            // 返回ack
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }

    }


    /**
     * 监听延迟队列中的消息（基于死信）
     */
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.QUEUE_DEAD_2)
    public void processDeadLetter(String msg,Message message,Channel channel){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("测试延时队列：收到消息时间：{}，消息：{}",sdf.format(new Date()),msg);

        System.out.println(new String(message.getBody()));

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    /**
     * 监听消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routingKey.confirm"}
    ))
    public void process(String msg,Message message, Channel channel){
        // 从 Message 对象中获取消息体
        log.info("msg---------->{}",msg);
        log.info("message.getBody()---------->{}",new String(message.getBody()));


        // 确认消息
        /*
            参数1：消息的id
            参数2：是否批量确认
         */
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
