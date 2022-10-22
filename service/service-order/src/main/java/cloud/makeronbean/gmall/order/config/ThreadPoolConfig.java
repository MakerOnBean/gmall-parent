package cloud.makeronbean.gmall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @author makeronbean
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService executorService() {
        return new ThreadPoolExecutor(
                50,
                500,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
                );
    }
}
