package cloud.makeronbean.gmall.product;

import cloud.makeronbean.gmall.common.constant.RedisConst;
import cloud.makeronbean.gmall.product.prop.MinioProp;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
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
public class ServiceProductApplication implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;

    public static void main(String[] args) {
        SpringApplication.run(ServiceProductApplication.class, args);
    }

    /**
     * 初始化布隆过滤器，指定数据规模和可以接受的误判率
     *      在保存时向布隆过滤器中添加映射
     *      在查询时通过布隆过滤器检查数据库中是否存在对应的key
     */
    @Override
    public void run(String... args) throws Exception {
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        bloomFilter.tryInit(1000L,0.01);
    }
}
