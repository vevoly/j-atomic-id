package io.github.vevoly.id.api.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * <h3>ID 申请结果 (Allocation Result)</h3>
 *
 * <p>
 * 服务端返回分配的 ID 号段范围。
 * 范围区间为：[minId, maxId] (闭区间)。
 * </p>
 *
 * <hr>
 *
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Allocation Result.</b><br>
 * Represents the allocated ID range returned by the server.<br>
 * Range: [minId, maxId] (Inclusive).
 * </span>
 *
 * @author vevoly
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否成功 (Is Success).
     */
    private boolean success;

    /**
     * 消息/错误码 (Message/Error Code).
     */
    private String message;

    /**
     * 起始 ID (含) (Start ID - Inclusive).
     */
    private long minId;

    /**
     * 结束 ID (含) (End ID - Inclusive).
     */
    private long maxId;

    /**
     * 快速构建成功结果.
     */
    public static AllocResult success(long minId, long maxId) {
        return new AllocResult(true, "OK", minId, maxId);
    }

    /**
     * 快速构建失败结果.
     */
    public static AllocResult fail(String message) {
        return new AllocResult(false, message, 0, 0);
    }
}
