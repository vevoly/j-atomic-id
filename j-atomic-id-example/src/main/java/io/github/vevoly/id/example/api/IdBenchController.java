package io.github.vevoly.id.example.api;

import io.github.vevoly.id.client.core.generator.IdGenerator;
import io.github.vevoly.id.example.service.OrderIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
public class IdBenchController {

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private OrderIdService orderIdService;

    /**
     * 压测接口
     * URL: http://localhost:8091/bench?mode=strict&count=1000000&threads=50
     * @param mode    模式: segment 或 strict
     * @param count   请求总数
     * @param threads 并发线程数
     */
    @GetMapping("/bench")
    public String bench(@RequestParam(value = "mode", defaultValue = "segment") String mode,
                        @RequestParam(value = "count", defaultValue = "1000000") int count,
                        @RequestParam(value = "threads", defaultValue = "50") int threads) {

        String bizTag = "segment".equals(mode) ? "bench_segment" : "bench_strict";
        log.info(">>> 开始压测 [{}] 模式, Tag={}, 总量={}, 线程={}", mode, bizTag, count, threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        // 每个线程需要执行的任务量
        int requestsPerThread = count / threads;
        int remainder = count % threads;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads; i++) {
            final int loopCount = (i == threads - 1) ? requestsPerThread + remainder : requestsPerThread;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < loopCount; j++) {
                        // 核心调用
                        long id = idGenerator.nextId(bizTag);
                        // 防止JVM过度优化去掉死代码，稍微用一下返回值
                        if (id < 0) throw new RuntimeException("ID Error");
                    }
                } catch (Exception e) {
                    log.error("压测异常", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(); // 等待所有线程完成
        } catch (InterruptedException e) {
            return "压测被中断";
        }

        long endTime = System.currentTimeMillis();
        long cost = endTime - startTime;
        long tps = (long) count * 1000 / (cost == 0 ? 1 : cost);

        String result = String.format("""
                === 压测报告 (%s) ===
                Tag: %s
                总请求: %d
                总耗时: %d ms
                并发线程: %d
                -------------------
                TPS: %d
                ===================
                """, mode, bizTag, count, cost, threads, tps);

        log.info("\n" + result);
        executor.shutdown();
        return result;
    }

    /**
     * 订单号压测接口 (带字符串拼接)
     * URL: http://localhost:8091/bench-order?count=1000000&threads=50
     */
    @GetMapping("/bench-order")
    public String benchOrder(@RequestParam(value = "count", defaultValue = "1000000") int count,
                             @RequestParam(value = "threads", defaultValue = "50") int threads) {

        log.info(">>> 开始订单号压测 | 总量: {} | 线程: {}", count, threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        int requestsPerThread = count / threads;
        int remainder = count % threads;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads; i++) {
            final int threadIndex = i; // 记录线程索引
            final int loopCount = (i == threads - 1) ? requestsPerThread + remainder : requestsPerThread;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < loopCount; j++) {
                        // 生成带格式的订单号
                        String orderNo = orderIdService.nextOrderNo();

                        // 抽样打印
                        // 只打印第 0 号线程的前 5 个 ID，看看长什么样
                        if (threadIndex == 0 && j < 5) {
                            log.info("🔥 订单号样例: {}", orderNo);
                        }

                        // 打印最后一条，确认 ID 增长到了哪里
                        if (threadIndex == 0 && j == loopCount - 1) {
                            log.info("🔚 本线程最后一条: {}", orderNo);
                        }
                    }
                } catch (Exception e) {
                    log.error("压测异常", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            return "Interrupted";
        }

        long endTime = System.currentTimeMillis();
        long cost = endTime - startTime;
        long safeCost = cost == 0 ? 1 : cost;
        long tps = (long) count * 1000 / safeCost;

        String result = String.format("""
                === 订单号压测报告 (Segment + String Format) ===
                Tag: bench_order
                格式: ORD-yyyyMMdd-RawID
                总请求: %d
                总耗时: %d ms
                并发线程: %d
                -------------------
                TPS: %d
                ===================
                """, count, cost, threads, tps);

        log.info("\n" + result);
        executor.shutdown();
        return result;
    }
}
