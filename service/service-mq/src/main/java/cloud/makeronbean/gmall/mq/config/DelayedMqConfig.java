package cloud.makeronbean.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author makeronbean
 */
@Configuration
public class DelayedMqConfig {
    public static final String EXCHANGE_DELAY = "exchange.delay";
    public static final String ROUTING_DELAY = "routing.delay";
    public static final String QUEUE_DELAY = "queue.delay.1";


    /**
     * 延迟队列创建
     */
    @Bean
    public Queue delayQueue() {
        return new Queue(QUEUE_DELAY,true);
    }

    /**
     * 延迟交换机创建
     * 交换机配置写死即可
     * 交换机第二个参数为交换机类型，写死即可
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String,Object> map = new HashMap<>(1);
        // 交换机固定配置
        map.put("x-delayed-type", "direct");

        return new CustomExchange(EXCHANGE_DELAY,"x-delayed-message",true,false,map);
    }

    /**
     * 绑定关系对象创建时，最后必须调用 noargs()方法
     */
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(ROUTING_DELAY).noargs();
    }
}
