package cloud.makeronbean.gmall.order.config;

import cloud.makeronbean.gmall.constant.MqConst;
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
public class OrderCancelMqConfig {

    /**
     * 延迟队列
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true,false,false);
    }

    /**
     * 延迟交换机
     */
    @Bean
    public CustomExchange orderExchange() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }

    /**
     * 绑定关系
     */
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
