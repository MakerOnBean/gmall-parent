package cloud.makeronbean.gmall.model;

import lombok.Data;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@Data
public class GmallCorrelationData extends CorrelationData {
    // 消息主体
    private Object message;
    // 交换机
    private String exchange;
    // routingKey
    private String routingKey;
    // 重试次数
    private int retryCount = 0;
    // 是否是延迟队列
    private boolean isDelay = false;
    // 延迟时间
    private int delayTime = 10;
}
