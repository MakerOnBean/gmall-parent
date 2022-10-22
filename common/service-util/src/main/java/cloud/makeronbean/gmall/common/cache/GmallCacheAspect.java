package cloud.makeronbean.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author makeronbean
 */
@Component
@Aspect
public class GmallCacheAspect {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 从缓存中查询并通过redisson添加分布式锁
     */
    @Around("@annotation(cloud.makeronbean.gmall.common.cache.GmallCache)")
    public Object gmallCacheAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 获取key所需要的前缀、后缀、参数等
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
            Object[] args = joinPoint.getArgs();
            String key = gmallCache.prefix() + Arrays.asList(args) + gmallCache.suffix();
            //从缓存中查询数据
            Object object = getByRedis(key,signature);
            if (object == null) {
                // redis中没有数据
                String lockKey = gmallCache.prefix() + Arrays.asList(args) + gmallCache.lockSuffix();

                //尝试加锁
                RLock lock = redissonClient.getLock(lockKey);
                boolean flag = lock.tryLock(Long.parseLong(gmallCache.tryLockWaitTime()), Long.parseLong(gmallCache.lessTime()), TimeUnit.SECONDS);
                // 成功获取到锁
                if (flag) {
                    try {
                        // 查询db数据库
                        object = joinPoint.proceed(args);
                        if (object != null){
                            // 查询数据库有值
                            redisTemplate.opsForValue().set(key,JSON.toJSONString(object),Long.parseLong(gmallCache.keyTimeOut()) + new Random().nextInt(300),TimeUnit.SECONDS);
                        } else {
                            // 查询数据库没有值
                            object = signature.getReturnType().newInstance();
                            redisTemplate.opsForValue().set(key, JSON.toJSONString(object),600L,TimeUnit.SECONDS);
                        }
                        return object;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // 没有获取到锁，自旋重试获取锁
                    Thread.sleep(500);
                    return gmallCacheAspect(joinPoint);
                }
            } else {
                // redis中有数据
                return object;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // 兜底方案，如果上面执行出现异常，则直接去查询数据库
        return joinPoint.proceed(joinPoint.getArgs());
    }

    /**
     * 从redis中查询数据
     */
    private Object getByRedis(String key, MethodSignature signature) {
        String strJson = redisTemplate.opsForValue().get(key);
        Class returnType = signature.getReturnType();
        if (!StringUtils.isEmpty(strJson)){
            return JSON.parseObject(strJson,returnType);
        } else {
            return null;
        }
    }

}
