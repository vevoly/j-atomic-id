package io.github.vevoly.id.client.starter;

import io.github.vevoly.id.client.config.IdClientProperties;
import io.github.vevoly.id.client.core.IdRemoteService;
import io.github.vevoly.id.client.core.generator.IdGenerator;
import io.github.vevoly.id.client.core.generator.SmartIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * <h3>客户端自动装配类 (Client Auto-Configuration)</h3>
 *
 * <p>
 * 负责初始化 ID 生成器客户端所需的所有组件。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Client Auto-Configuration.</b><br>
 * Initializes all components required by the ID Generator Client.
 * </span>
 *
 * @author vevoly
 */
@Configuration
@EnableConfigurationProperties(IdClientProperties.class)
public class JAtomicIdClientAutoConfiguration {

    /**
     * 初始化 RestTemplate (HTTP 客户端).
     * <p>
     * 如果用户没有定义 RestTemplate，则创建一个默认的，并设置 3秒 超时。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 初始化远程服务 (Remote Service).
     */
    @Bean
    @ConditionalOnMissingBean
    public IdRemoteService idRemoteService(RestTemplate restTemplate, IdClientProperties properties) {
        return new IdRemoteService(restTemplate, properties);
    }

    /**
     * 初始化智能 ID 生成器 (Smart ID Generator).
     * <p>
     * 这是用户主要使用的 Bean。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(IdRemoteService remoteService, IdClientProperties properties) {
        return new SmartIdGenerator(remoteService, properties);
    }
}
