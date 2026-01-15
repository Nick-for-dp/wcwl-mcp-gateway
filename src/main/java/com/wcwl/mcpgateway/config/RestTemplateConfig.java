package com.wcwl.mcpgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 * 
 * <p>配置用于调用第三方服务的 HTTP 客户端。</p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean
     * 
     * <p>RestTemplate 是 Spring 提供的 HTTP 客户端，用于调用 REST API。</p>
     * 
     * @return 配置好的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：10秒
        factory.setConnectTimeout(10000);
        // 读取超时：30秒
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }
}
