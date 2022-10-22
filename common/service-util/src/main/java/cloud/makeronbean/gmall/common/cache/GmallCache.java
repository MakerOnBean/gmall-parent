package cloud.makeronbean.gmall.common.cache;

import java.lang.annotation.*;

/**
 *
 * 源注解：
 *  @Target： 限制该注解可以标示的位置，方法上、属性上、类上等
 *  @Retention： 注解的生命周期
 *                  SOURCE： 只存在与源文件中（.java文件）
 *                  CLASS: 可以存活到编译为class文件
 *                  RUNTIME： 运行时依然存在
 * @Inherited： 被当前注解（@GmallCache）修饰的类，其子类也会有该注解的属性
 * @Documented： 标示的注解可以被javadoc编译为文档
 *
 * @author makeronbean
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {

    /**
     * 缓存中key的前缀
     */
    String prefix() default "cache:";

    /**
     * 缓存中key的后缀
     */
    String suffix() default ":info";

    /**
     * 缓存中key的后缀
     */
    String lockSuffix() default ":lock";

    /**
     * 尝试获取锁最大时间
     */
    String tryLockWaitTime() default "100";

    /**
     * 持有锁的最大时间
     */
    String lessTime() default "10";

    /**
     * 缓存中保存最大时间
     */
    String keyTimeOut() default "86400";
}
