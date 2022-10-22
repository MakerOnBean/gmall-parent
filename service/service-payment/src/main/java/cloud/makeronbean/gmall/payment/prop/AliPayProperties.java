package cloud.makeronbean.gmall.payment.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author makeronbean
 */
@Data
@ConfigurationProperties(prefix = "alipay")
public class AliPayProperties {
    /**
     * 阿里支付公钥
     */
    private String alipayPublicKey;

    /**
     * 阿里支付请求接口路径
     */
    private String alipayUrl;

    /**
     * 自己应用的id
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String appPrivateKey;

    /**
     * 返回值格式
     */
    private String format;

    /**
     * 字符集编码
     */
    private String charset;

    /**
     * 签名加密方式
     */
    private String signType;

    /**
     * 同步回调方法
     */
    private String returnPaymentUrl;

    /**
     * 支付成功页面
     */
    private String returnOrderUrl;

    /**
     * 异步回调方法
     */
    private String notifyPaymentUrl;


}
