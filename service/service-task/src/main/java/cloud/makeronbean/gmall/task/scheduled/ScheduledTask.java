package cloud.makeronbean.gmall.task.scheduled;

import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @EnableScheduling 开启定时任务支持
 *
 * @author makeronbean
 */
@Component
@Slf4j
@EnableScheduling
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;


    /**
     * 创建定时任务
     * 用于每天凌晨一点将需要秒杀的商品放入缓存中预热
     */
    @Scheduled(cron = "0/15 * * * * ?")
    public void task1() {
        // 定时发送消息
        rabbitService.send(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    }


    /**
     * 定期删除过期的秒杀数据缓存
     */
    @Scheduled(cron = "0/40 * * * * ?")
    public void task18() {
        rabbitService.send(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"");
    }
}
