package io.github.vevoly.id.client.core.generator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <h3>号段模型 (Segment Model)</h3>
 * <p>代表从服务端申请回来的一批 ID。</p>
 */
public class Segment {

    // 当前游标 (Atomic 保证线程安全)
    private final AtomicLong cursor;

    // 最大值 (当前号段的终点)
    private final long maxId;

    // 号段步长 (用于计算预加载阈值)
    private final int step;

    public Segment(long minId, long maxId) {
        this.cursor = new AtomicLong(minId);
        this.maxId = maxId;
        this.step = (int) (maxId - minId + 1);
    }

    /**
     * 尝试获取 ID
     * @return 如果 <= maxId，返回 ID；否则返回 -1 (表示耗尽)
     */
    public long getNextId() {
        long id = cursor.getAndIncrement();
        if (id > maxId) {
            return -1; // 耗尽
        }
        return id;
    }

    /**
     * 获取当前消耗比例 (0.0 - 1.0)
     * 用于判断是否需要触发预加载
     */
    public double getUsedRatio() {
        // cursor 已经加过了，所以要减回来算当前
        long current = cursor.get() - 1;
        long min = maxId - step + 1;
        return (double) (current - min) / step;
    }
}
