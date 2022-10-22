package cloud.makeronbean.gmall.payment;

import cloud.makeronbean.gmall.payment.prop.AliPayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author makeronbean
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "cloud.makeronbean.gmall")
@ComponentScan(basePackages = "cloud.makeronbean.gmall")
@EnableConfigurationProperties({AliPayProperties.class})
public class ServicePaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicePaymentApplication.class, args);
    }
}
