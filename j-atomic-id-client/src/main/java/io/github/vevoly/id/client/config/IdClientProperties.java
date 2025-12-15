package io.github.vevoly.id.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * <h3>客户端配置属性 (Client Configuration Properties)</h3>
 *
 * <p>
 * 对应 {@code application.yml} 中的 {@code j-atomic-id.client} 前缀。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Client Configuration Properties.</b><br>
 * Maps to {@code j-atomic-id.client} in application.yml.
 * </span>
 *
 * @author vevoly
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "j-atomic-id.client")
public class IdClientProperties {

    /**
     * 服务端地址 (Server URL).
     */
    private String serverUrl;

    /**
     * 应用 ID (App ID).
     * <p>用于未来鉴权或统计 (Optional).</p>
     */
    private String appId;

    /**
     * 业务规则列表 (Business Rules).
     * <p>针对不同的业务 tag 配置不同的生成策略。</p>
     */
    private List<Rule> rules = new ArrayList<>();

    /**
     * 单个业务规则配置
     */
    @Data
    public static class Rule {

        /**
         * 业务标识 (Business Tag).
         * <p>必填。例如: "order", "user_id", "chat_group_*" (支持前缀匹配后续可扩展)</p>
         */
        private String bizTag;

        /**
         * 生成模式 (Generation Mode).
         * <p>默认为 SEGMENT (号段模式)。</p>
         */
        private IdMode mode = IdMode.SEGMENT;

        /**
         * 号段步长 (Step Size).
         * <p>仅在 SEGMENT 模式下生效。表示一次向服务端申请多少个 ID。<br>
         * 默认: 1000。</p>
         */
        private int step = 1000;

        /**
         * 预加载阈值比例 (Pre-load Ratio).
         * <p>仅在 SEGMENT 模式下生效。范围 0.0 - 1.0。<br>
         * 当当前号段剩余量低于 (step * ratio) 时，触发异步预加载。<br>
         * 默认: 0.4 (剩余 40% 时加载)。</p>
         */
        private double minBufferRatio = 0.4;

        /**
         * 格式化规则 (Format Pattern).
         * <p>可选。例如: "ORD-{yyyyMMdd}-{seq}"。如果不填则返回原始 long ID。</p>
         */
        private String format;
    }
}
