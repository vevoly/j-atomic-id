package io.github.vevoly.id.server.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <h3>ID 生成器内存状态 (ID Generator Memory State)</h3>
 *
 * <p>核心数据结构，存储所有业务 Tag 当前已分配到的最大 ID。</p>
 *
 * @author vevoly
 */
@Data
public class IdState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID 计数器存储.
     * <ul>
     *     <li>Key: bizTag (业务标识，如 "order_01", "chat_group_100")</li>
     *     <li>Value: currentMaxId (当前已分配的最大 ID)</li>
     * </ul>
     */
    private Map<String, Long> sequences = new HashMap<>();
}
