package cloud.makeronbean.gmall.item;

import cloud.makeronbean.gmall.common.config.RedissonConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author makeronbean
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
// 扫描feign接口
@EnableFeignClients(basePackages = "cloud.makeronbean.gmall")
@ComponentScan(basePackages = "cloud.makeronbean.gmall")
@EnableDiscoveryClient
public class ServiceItemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceItemApplication.class, args);
    }
}
