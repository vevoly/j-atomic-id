package io.github.vevoly.id.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class Retryer {

    /**
     * 执行带重试的任务 (Execute with Retry).
     *
     * @param task          具体任务逻辑 (The task to execute)
     * @param maxAttempts   最大重试次数 (Max attempts)
     * @param backoffMillis 重试间隔毫秒 (Backoff time in ms)
     * @param <T>           返回值类型
     * @return 任务执行结果
     * @throws Exception 如果重试耗尽依然失败，抛出最后一次的异常
     */
    public static <T> T execute(Callable<T> task, int maxAttempts, long backoffMillis) throws Exception {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("任务执行失败，正在重试 ({}/{})，异常: {}", attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                }
            }
        }
        // 重试耗尽，抛出异常
        log.error("[j-atomic-id-client] All retry attempts failed.");
        throw lastException;
    }
}
