package io.github.vevoly.id.client.core.generator;

/**
 * <h3>ID 生成器接口 (ID Generator Interface)</h3>
 *
 * <p>
 * 客户端获取 ID 的统一入口。
 * 具体实现可能是基于内存缓冲的 {@link SegmentIdGenerator}，也可能是实时请求的 {@link StrictIdGenerator}。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>ID Generator Interface.</b><br>
 * Unified entry point for clients to get IDs.<br>
 * Implementations can be memory-buffered {@link SegmentIdGenerator} or real-time {@link StrictIdGenerator}.
 * </span>
 *
 * @author vevoly
 */
public interface IdGenerator {

    /**
     * 获取下一个 ID (Get Next ID).
     *
     * @param bizTag 业务标识 (Business Tag)
     * @return 唯一的 long 类型 ID
     */
    long nextId(String bizTag);
}

