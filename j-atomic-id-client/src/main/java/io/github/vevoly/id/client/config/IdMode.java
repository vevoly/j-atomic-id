package io.github.vevoly.id.client.config;

/**
 * <h3>ID 生成模式枚举 (ID Generation Mode)</h3>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>ID Generation Mode Enum.</b><br>
 * Defines how the client SDK interacts with the server.
 * </span>
 *
 * @author vevoly
 * @since 1.0.0
 */
public enum IdMode {
    /**
     * <b>号段缓冲模式 (Segment Buffer Mode)</b>
     * <p>
     * 适用于：订单、商品 ID 等不需要严格连续，但追求极致性能的场景。
     * <br>客户端会批量申请 ID (如 1000 个) 缓存在本地内存，并在用尽前自动预加载下一段。
     * </p>
     * <span style="color: gray;">Best for high throughput (Orders). Uses local double-buffer to pre-load IDs. Zero network latency.</span>
     */
    SEGMENT,

    /**
     * <b>严格连续模式 (Strict Sequential Mode)</b>
     * <p>
     * 适用于：IM 消息 ID、Feed 流等必须严格递增、无空洞的场景。
     * <br>每次获取 ID 都会实时发起网络请求 (step=1)。
     * </p>
     * <span style="color: gray;">Best for strict consistency (IM Messages). Real-time network request for every ID. No gaps.</span>
     */
    STRICT
}

