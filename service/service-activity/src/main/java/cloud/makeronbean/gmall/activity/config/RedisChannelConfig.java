package cloud.makeronbean.gmall.activity.config;

import cloud.makeronbean.gmall.activity.recevier.MessageReceive;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author makeronbean
 */
@Configuration
public class RedisChannelConfig {
    /**
     * 订阅主题
     * 参数1：redis连接工厂
     * 参数2：消息监听适配器
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory,
                                                   MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();

        // 设置redis连接工厂
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);

        // 设置消息适监听，并订阅主题
        redisMessageListenerContainer.addMessageListener(messageListenerAdapter,new PatternTopic(RedisConst.SECKILL_TOPIC));

        return redisMessageListenerContainer;
    }



    /**
     * 配置消息监听适配器
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(MessageReceive receive) {
        // 参数一为消息接收对象
        // 参数二为接收对象的消息处理方法
        return new MessageListenerAdapter(receive,"receiveMessage");
    }

    /**
     * 容器中注入redisTemplate对象
     */
    @Bean
    public StringRedisTemplate template(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
