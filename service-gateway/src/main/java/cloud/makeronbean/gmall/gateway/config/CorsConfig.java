package cloud.makeronbean.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author makeronbean
 */
@Configuration
public class CorsConfig {

    /**
     * 全局跨域处理
     * 配置跨域资源共享
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许跨域的路径
        configuration.addAllowedOrigin("*");
        // 允许跨域的方法
        configuration.addAllowedMethod("*");
        // 允许跨域携带的头信息
        configuration.addAllowedHeader("*");
        // 是否允许携带cookie
        configuration.setAllowCredentials(true);

        // 创建配置对象
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        // 设置配置
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",configuration);

        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
