package cloud.makeronbean.gmall.payment.config;

import cloud.makeronbean.gmall.payment.prop.AliPayProperties;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author makeronbean
 */
@Configuration
public class AliPayConfig {

    @Autowired
    private AliPayProperties aliPayProperties;


    /**
     * 注入阿里支付连接对象
     */
    @Bean
    public AlipayClient alipayClient() {

        return new DefaultAlipayClient(aliPayProperties.getAlipayUrl(),
                aliPayProperties.getAppId(), aliPayProperties.getAppPrivateKey(),aliPayProperties.getFormat(),
                aliPayProperties.getCharset(),aliPayProperties.getAlipayPublicKey(),aliPayProperties.getSignType());
    }
}
