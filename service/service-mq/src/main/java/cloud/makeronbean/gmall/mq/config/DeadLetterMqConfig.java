package cloud.makeronbean.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 延迟队列组件注入
 * 通过ttl与死信队列实现
 * @author makeronbean
 */
@Configuration
public class DeadLetterMqConfig {

    public static final String EXCHANGE_DEAD = "exchange.dead";
    public static final String ROUTING_DEAD_1 = "routing.dead.1";
    public static final String ROUTING_DEAD_2 = "routing.dead.2";
    public static final String QUEUE_DEAD_1 = "queue.dead.1";
    public static final String QUEUE_DEAD_2 = "queue.dead.2";

    /**
     * 队列1
     * 延迟队列
     * Queue构造方法参数说明
     *  参数1：队列名称
     *  参数2：是否持久化
     *  参数3：是否独享
     *  参数4：是否自动删除队列
     *  参数5：创建队列时锁需要的队列参数
     */
    @Bean
    public Queue deadQueue1(){
        Map<String,Object> map = new HashMap<>(3);
        // 设置队列中消息的过期时间
        map.put("x-message-ttl",1000*10);

        // 绑定该队列的消息过期后，转发的交换机一级routingKey
        map.put("x-dead-letter-exchange",EXCHANGE_DEAD);
        map.put("x-dead-letter-routing-key",ROUTING_DEAD_2);

        return new Queue(QUEUE_DEAD_1,true,false,false,map);
    }

    /**
     * 队列2
     * 死信队列
     * 绑定消费者的交换机
     */
    @Bean
    public Queue deadQueue2(){
        return new Queue(QUEUE_DEAD_2,true,false,false,null);
    }

    /**
     * 交换机
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DEAD,true,false);
    }

    /**
     * 绑定交换机与延迟队列
     */
    @Bean
    public Binding binding1() {
        return BindingBuilder.bind(deadQueue1()).to(directExchange()).with(ROUTING_DEAD_1);
    }

    /**
     * 绑定交换机与死信队列
     */
    @Bean
    public Binding binding2() {
        return BindingBuilder.bind(deadQueue2()).to(directExchange()).with(ROUTING_DEAD_2);
    }
}
