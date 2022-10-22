package cloud.makeronbean.gmall.service;

import cloud.makeronbean.gmall.model.GmallCorrelationData;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author makeronbean
 */
@Service
//@Slf4j
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 抽取发送消息方法
     */
    public boolean send(String exchange, String routingKey, Object message) {

        // 发送消息前，构建实体类
        GmallCorrelationData gmallCorrelationData = this.buildGmallCorrelationData(exchange,routingKey,message,false,null);

        // 存储到redis
        redisTemplate.opsForValue().set(Objects.requireNonNull(gmallCorrelationData.getId()), JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);

        // 发送消息时将 CorrelationData 对象传入
        rabbitTemplate.convertSendAndReceive(exchange,routingKey,message,gmallCorrelationData);
        return true;
    }


    /**
     * 抽象延迟消息发送方法
     */
    public boolean sendDelay(String exchange, String routingKey, Object message, Integer delay) {
        // 发送消息前，构建实体类
        GmallCorrelationData gmallCorrelationData = this.buildGmallCorrelationData(exchange,routingKey,message,true,delay);

        // 存储到redis
        redisTemplate.opsForValue().set(Objects.requireNonNull(gmallCorrelationData.getId()), JSON.toJSONString(gmallCorrelationData),10,TimeUnit.MINUTES);

        // 发送消息时将 延迟时间 与 CorrelationData 对象传入
        rabbitTemplate.convertSendAndReceive(exchange,routingKey,message,message1 -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //log.info("测试延时队列：发送消息时间：{}，消息：{}",sdf.format(new Date()),message);
            System.out.println(sdf.format(new Date())+":"+message);

            message1.getMessageProperties().setDelay(delay * 1000);
            return message1;
        }, gmallCorrelationData);
        return true;
    }


    /**
     * 封装 GmallCorrelationData 对象
     */
    private GmallCorrelationData buildGmallCorrelationData(String exchange, String routingKey, Object message, Boolean isDelay, Integer delay) {
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        // 设置id
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        gmallCorrelationData.setId(id);
        // 设置消息主体
        gmallCorrelationData.setMessage(message);
        // 设置交换机
        gmallCorrelationData.setExchange(exchange);
        // 设置routingKey
        gmallCorrelationData.setRoutingKey(routingKey);
        // 是否是延迟消息
        if (isDelay){
            gmallCorrelationData.setDelay(true);
            gmallCorrelationData.setDelayTime(delay);
        }
        return gmallCorrelationData;
    }

}
