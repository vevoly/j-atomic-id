package io.github.vevoly.id.api.domain;

import io.github.vevoly.ledger.api.BaseLedgerCommand;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

/**
 * <h3>ID 申请命令 (ID Allocation Command)</h3>
 *
 * <p>
 * 客户端向服务端发起 ID 申请请求的载体。
 * 复用了父类的 {@code amount} 字段作为本次申请的 ID 数量 (Step)。
 * </p>
 *
 * <hr>
 *
 * <span style="color: gray; font-size: 0.9em;">
 * <b>ID Allocation Command.</b><br>
 * Request payload for allocating IDs from the server.<br>
 * Reuses the parent class's {@code amount} field to represent the allocation count (Step).
 * </span>
 *
 * @author vevoly
 * @since 1.0.0
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AllocCommand extends BaseLedgerCommand {

    /**
     * 业务标识 (Business Tag).
     * <p>例如: "order", "user_id", "chat_group_10086"</p>
     * <span style="color: gray;">Business identifier key.</span>
     */
    private String bizTag;

    /**
     * 获取路由键 (Get Routing Key).
     * <p>
     * <b>核心逻辑：</b> 使用 {@code bizTag} 作为路由键。<br>
     * 这样可以保证同一个业务类型的 ID 申请请求，永远被路由到同一个 Disruptor 分片中处理。<br>
     * 从而实现该业务类型下的 ID <b>严格递增</b> 和 <b>绝对无锁</b>。
     * </p>
     */
    @Override
    public String getRoutingKey() {
        return bizTag;
    }

    /**
     * 序列化扩展：写入业务字段 (bizTag).
     */
    @Override
    protected void writeBizData(BytesOut<?> bytes) {
        bytes.writeUtf8(bizTag);
    }

    /**
     * 反序列化扩展：读取业务字段 (bizTag).
     */
    @Override
    protected void readBizData(BytesIn<?> bytes) {
        this.bizTag = bytes.readUtf8();
    }

    // 辅助方法：获取申请数量 (语义更清晰)
    public int getCount() {
        return (int) this.amount;
    }

    // 辅助方法：设置申请数量
    public void setCount(int count) {
        this.amount = count;
    }
}
