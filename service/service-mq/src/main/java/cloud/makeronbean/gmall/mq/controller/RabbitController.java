package cloud.makeronbean.gmall.mq.controller;

import cloud.makeronbean.gmall.common.result.Result;
import cloud.makeronbean.gmall.mq.config.DeadLetterMqConfig;
import cloud.makeronbean.gmall.mq.config.DelayedMqConfig;
import cloud.makeronbean.gmall.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author makeronbean
 */
@RestController
@RequestMapping("/mq")
@Slf4j
public class RabbitController {
    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送延迟消息（插件，封装）
     */
    @GetMapping("/sendDelayed2")
    public Result sendDelayed2() {

        rabbitService.sendDelay(DelayedMqConfig.EXCHANGE_DELAY,DelayedMqConfig.ROUTING_DELAY,"基于延迟队列实现的延迟消息",10);

        return Result.ok();
    }


    /**
     * 发送延迟消息（插件，未封装）
     */
    @GetMapping("/sendDelayed")
    public Result sendDelayed() {

        rabbitTemplate.convertSendAndReceive(DelayedMqConfig.EXCHANGE_DELAY, DelayedMqConfig.ROUTING_DELAY, "基于延迟队列实现的延迟消息", new MessagePostProcessor() {

            /**
             * 相当于拦截器，当发送消息时，会调用该方法
             * 可以在这里设置消息延迟发送时间
             */
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                log.info("测试延时队列：发送消息时间：{}，消息：{}",sdf.format(new Date()),"基于延迟队列实现的延迟消息");
                message.getMessageProperties().setDelay(1000*10);

                return message;
            }
        });

        return Result.ok();
    }


    /**
     * 发送延迟消息（死信队列）
     */
    @GetMapping("/sendDeadLetter")
    public Result sendDeadLetter(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("测试延时队列：发送消息时间：{}，消息：{}",sdf.format(new Date()),"延时消息1");
        rabbitService.send(DeadLetterMqConfig.EXCHANGE_DEAD,DeadLetterMqConfig.ROUTING_DEAD_1,"延时消息1");
        return Result.ok();
    }


    /**
     * 发送消息
     */
    @GetMapping("/send")
    public Result send(){
        rabbitService.send("exchange.confirm","routingKey.confirm","你好");
        return Result.ok();
    }
}
