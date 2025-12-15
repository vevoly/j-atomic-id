package io.github.vevoly.id.client.core.generator;

import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.client.config.IdClientProperties;
import io.github.vevoly.id.client.core.IdRemoteService;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <h3>双 Buffer 缓冲控制器 (Double Buffer Controller)</h3>
 *
 * <p>
 * 实现了<b>"双 Buffer + 异步预加载"</b>机制，确保高并发下获取 ID <b>零延迟</b>。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Double Buffer Controller.</b><br>
 * Implements "Double Buffer + Async Pre-load" mechanism to ensure <b>Zero Latency</b>.
 * </span>
 */
@Slf4j
public class SegmentBuffer {

    private final String bizTag;
    private final IdRemoteService remoteService;
    private final ExecutorService asyncLoader; // 异步线程池

    // 配置参数
    private final int step;
    private final double minBufferRatio;

    // --- 核心状态 ---
    private volatile Segment current; // 当前正在使用的号段
    private volatile Segment next;    // 下一个备用号段

    private final AtomicBoolean isLoadingNext = new AtomicBoolean(false); // 是否正在预加载
    private final Lock switchLock = new ReentrantLock(); // 切换锁

    public SegmentBuffer(String bizTag, IdClientProperties.Rule rule, IdRemoteService remoteService, ExecutorService asyncLoader) {
        this.bizTag = bizTag;
        this.step = rule.getStep();
        this.minBufferRatio = rule.getMinBufferRatio();
        this.remoteService = remoteService;
        this.asyncLoader = asyncLoader;

        // 初始化：同步加载第一个号段 (构造时必须可用)
        log.info("初始化号段: {}", bizTag);
        AllocResult result = remoteService.alloc(bizTag, step);
        this.current = new Segment(result.getMinId(), result.getMaxId());
    }

    /**
     * 获取 ID (核心入口)
     */
    public long nextId() {
        while (true) {
            // 1. 尝试从 current 获取
            long id = current.getNextId();
            // 2. 如果获取成功
            if (id != -1) {
                // 检查是否需要预加载 next
                checkAndLoadNext();
                return id;
            }
            // 3. 如果耗尽，执行切换逻辑 (加锁防止并发切换)
            switchLock.lock();
            try {
                // Double Check: 再次尝试获取 (可能别的线程已经切换好了)
                id = current.getNextId();
                if (id != -1) {
                    return id;
                }
                // 真的耗尽了，检查 next 是否这就绪
                if (next == null) {
                    log.warn("[{}] Current exhausted and Next is null! Blocking wait...", bizTag);
                    // 这是一个"糟糕"的情况：消费太快，预加载没跟上
                    // 必须同步去加载 (降级为同步阻塞)
                    AllocResult result = remoteService.alloc(bizTag, step);
                    current = new Segment(result.getMinId(), result.getMaxId());
                } else {
                    // 正常切换：把 next 转正
                    log.info("[{}] Switching to next segment.", bizTag);
                    current = next;
                    next = null;
                    isLoadingNext.set(false); // 允许再次触发预加载
                }
            } finally {
                switchLock.unlock();
            }
        }
    }

    /**
     * 检查并触发异步预加载
     */
    private void checkAndLoadNext() {
        // 如果 next 已经有了，或者正在加载中，就不用管了
        if (next != null || isLoadingNext.get()) {
            return;
        }

        // 检查比例：如果当前用量超过阈值 (例如 40%)
        if (current.getUsedRatio() >= minBufferRatio) {
            // CAS 抢占加载权
            if (isLoadingNext.compareAndSet(false, true)) {
                asyncLoader.submit(() -> {
                    try {
                        log.debug("[{}] Async loading next segment...", bizTag);
                        AllocResult result = remoteService.alloc(bizTag, step);
                        next = new Segment(result.getMinId(), result.getMaxId());
                        log.debug("[{}] Next segment ready: {}-{}", bizTag, result.getMinId(), result.getMaxId());
                    } catch (Exception e) {
                        log.error("[{}] Async load failed", bizTag, e);
                        // 失败了把标记改回去，允许下次重试
                        isLoadingNext.set(false);
                    }
                });
            }
        }
    }
}
