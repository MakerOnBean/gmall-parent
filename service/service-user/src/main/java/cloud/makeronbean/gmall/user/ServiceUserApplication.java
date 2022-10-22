package cloud.makeronbean.gmall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author makeronbean
 */
@SpringBootApplication
//@EnableFeignClients(basePackages = "cloud.makeronbean.gmall")
@EnableDiscoveryClient
@ComponentScan(basePackages = "cloud.makeronbean.gmall")
public class ServiceUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceUserApplication.class, args);
    }

}