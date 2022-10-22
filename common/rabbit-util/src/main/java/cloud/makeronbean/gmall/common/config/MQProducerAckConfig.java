package cloud.makeronbean.gmall.common.config;

import cloud.makeronbean.gmall.model.GmallCorrelationData;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 用于绑定 生产者-》交换机 与 交换机 -》 队列 失败后的回调方法到 rabbitTemplate
 *
 * @author makeronbean
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * bean实例化完成后执行
     * 绑定RabbitTemplate与下面两个回调方法
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }

    /**
     * 从生产者发送到交换机时触发
     * @param correlationData 数据，必须在发送消息时传递一个correlationData对象，并且消息发送失败，才会有值
     * @param ack 是否发送成功
     * @param cause 发送失败原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("生产者 -> 交换机：消息发送成功");
        } else {
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
            this.retryMessage(correlationData);
        }
    }


    /**
     * 从交换机发送消息到队列 失败时触发（发送成功不触发）
     * 注意：使用延迟插件时，消息会在指定时间后才会放入队列中，所以队列会认为消息丢失，会调用该方法
     * @param message 消息主体
     * @param replyCode 应答码
     * @param replyText 描述
     * @param exchange 交换机
     * @param routingKey 路由key
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {


        // 获取 GmallCorrelationData 对象的 id
        String id = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");



        if (!StringUtils.isEmpty(id)) {

            // 反序列化对象输出
            log.info("交换机 -> 消息队列：消息发送失败");
            log.info("消息主体: {}",new String(message.getBody()));
            log.info("应答码: {}",replyCode);
            log.info("描述: {}",replyText);
            log.info("消息使用的交换器 exchange : {}",exchange);
            log.info("消息使用的路由键 routing : {}",routingKey);

            // 根据id从redis中查询
            String strJson = redisTemplate.opsForValue().get(id);

            GmallCorrelationData correlationData = JSON.parseObject(strJson, GmallCorrelationData.class);
            this.retryMessage(correlationData);
        }
    }


    /**
     * 重试发送消息
     * @param correlationData 自己封装的实体类，记录了交换机、路由key等，用于消息重试发送
     */
    private void retryMessage(CorrelationData correlationData) {
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;

        // 判断是否到达重试次数
        int retryCount = gmallCorrelationData.getRetryCount();
        if (retryCount >= 3) {
            log.error("消息：" + gmallCorrelationData.getMessage() + "------------->到达重试次数，发送失败");
            return;
        }

        // 重试次数++
        gmallCorrelationData.setRetryCount(++retryCount);

        // 更新redis中的缓存
        redisTemplate.opsForValue().set(gmallCorrelationData.getId(),JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);

        // 重试发送消息
        if (gmallCorrelationData.isDelay()) {
            // 如果是延迟队列
            rabbitTemplate.convertSendAndReceive(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), message -> {
                message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime() * 1000);
                return message;
            }, gmallCorrelationData);
        } else {
            // 如果不是延迟队列

            rabbitTemplate.convertSendAndReceive(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(), gmallCorrelationData.getMessage(), gmallCorrelationData);
        }

        log.info("重试次数------------> {}",retryCount);
    }
}
