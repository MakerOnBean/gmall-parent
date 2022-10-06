package cloud.makeronbean.gmall.product.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author makeronbean
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProp {
    public String endpointUrl;
    public String accessKey;
    public String secreKey;
    public String bucketName;
}
