package cloud.makeronbean.gmall.activity.recevier;

import cloud.makeronbean.gmall.activity.mapper.SeckillGoodsMapper;
import cloud.makeronbean.gmall.activity.service.SeckillGoodsService;
import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.common.util.DateUtil;
import cloud.makeronbean.gmall.constant.MqConst;
import cloud.makeronbean.gmall.model.activity.SeckillGoods;
import cloud.makeronbean.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author makeronbean
 */
@Component
public class SeckillRecevier {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    /**
     * 监听定时任务清除过期缓存数据
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER,durable = "true",autoDelete = "false"),
            key = MqConst.QUEUE_TASK_18
    ))
    @SneakyThrows
    public void clearRedis(Message message, Channel channel) {
        try {
            LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
            wrapper.le(SeckillGoods::getEndTime,new Date());
            wrapper.eq(SeckillGoods::getStatus,1);
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);


            // 删除临时订单
            redisTemplate.delete(RedisConst.SECKILL_ORDERS);

            // 删除用户订单
            redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

            // 遍历删除
            if (!CollectionUtils.isEmpty(seckillGoodsList)) {
                seckillGoodsList.forEach(seckillGoods -> {
                    // 删除删除预热数据--goods列表
                    redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).delete(seckillGoods.getSkuId().toString());

                    // 删除商品剩余数list
                    redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());


                    // 修改数据库中秒杀商品的状态
                    seckillGoods.setStatus("2");
                    seckillGoodsMapper.updateById(seckillGoods);
                });
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    /**
     * 秒杀下单监听
     * 实现秒杀下单
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER,durable = "true", autoDelete = "false"),
            key = MqConst.ROUTING_SECKILL_USER
    ))
    @SneakyThrows
    public void seckillUser(UserRecode userRecode, Message message, Channel channel) {
        try {
            if (userRecode != null) {
                seckillGoodsService.seckillUser(userRecode.getUserId(),userRecode.getSkuId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


    /**
     * 读取定时任务发送的消息
     * 将需要秒杀的商品预热到redis中
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK,durable = "true", autoDelete = "false"),
            key = MqConst.ROUTING_TASK_1
    ))
    public void importToRedis(Message message, Channel channel) {
        // 查询数据库中，看有哪些数据需要被预热
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        // 库存大于0
        wrapper.gt("num",0);
        // 开始时间是今天
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        // 审核状态为1
        wrapper.eq("status","1");
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);

        if (!CollectionUtils.isEmpty(seckillGoodsList)) {
            // 将需要预热的商品放入redis中
            seckillGoodsList.forEach(seckillGoods -> {

                // 判断该秒杀数据是否已经存在redis中
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if (!flag) {
                    // 以hash的格式放入redis中， key：seckill:goods field：seckillGoodsId value: seckillGoods
                    redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,seckillGoods.getSkuId().toString(),seckillGoods);

                    // 将秒杀数量以list格式存储到redis中，key：seckill:stock:skuId    value：skuId
                    for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                        redisTemplate.opsForList().leftPush(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId().toString(),seckillGoods.getSkuId().toString());
                    }
                }

                // 发布消息到通道中
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId()+":1");

            });


        }
        // 确认消息
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
