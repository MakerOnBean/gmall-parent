package cloud.makeronbean.gmall.product;

import cloud.makeronbean.gmall.product.prop.MinioProp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author makeronbean
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = "cloud.makeronbean.gmall")
@EnableConfigurationProperties(MinioProp.class)
public class ServiceProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplication.class, args);
    }
}
