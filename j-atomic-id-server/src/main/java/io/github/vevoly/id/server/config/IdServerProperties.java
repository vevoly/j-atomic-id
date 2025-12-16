package io.github.vevoly.id.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h3>服务端配置属性 (Server Configuration Properties)</h3>
 *
 * <p>
 * 对应 {@code application.yml} 中的 {@code j-atomic-id.server} 前缀。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Server Configuration Properties.</b><br>
 * Maps to {@code j-atomic-id.server} in application.yml.
 * </span>
 *
 * @author vevoly
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "j-atomic-id.server")
public class IdServerProperties {

    private Auth auth = new Auth();

    @Data
    public static class Auth {

        /**
         * 是否开启鉴权
         */
        private boolean enabled = true;

        /**
         * 允许的客户端列表
         * Key: AppKey, Value: AppSecret
         */
        private Map<String, String> clients = new HashMap<>();

        /**
         * ip 白名单
         */
        private List<String> ipWhitelist = new ArrayList<>();
    }


}
