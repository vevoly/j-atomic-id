package io.github.vevoly.id.server.api;

import io.github.vevoly.id.api.domain.AllocCommand;
import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.api.exceptions.IdErrorCode;
import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import io.github.vevoly.id.server.domain.IdState;
import io.github.vevoly.ledger.core.LedgerEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping("/id")
public class IdController {

    @Autowired
    private LedgerEngine<IdState, AllocCommand, IdGeneratorEntity> engine;

    /**
     * 申请 ID (核心接口)
     * URL: POST /id/alloc?tag=order&count=100
     */
    @PostMapping("/alloc")
    public AllocResult alloc(@RequestParam(value = "tag") String tag,
                             @RequestParam(value = "count", defaultValue = "1") int count) {
        // 1. 构建命令
        AllocCommand cmd = new AllocCommand();
        cmd.setTxId(UUID.randomUUID().toString());
        cmd.setBizTag(tag);
        cmd.setCount(count);

        CompletableFuture<Object> future = new CompletableFuture<>();
        cmd.setFuture(future);

        try {
            // 2. 提交到单线程引擎
            engine.submit(cmd);
        } catch (Exception e) {
            log.error("[alloc] engine submit failed, tag={}", tag, e);
            return AllocResult.fail(IdErrorCode.SERVER_BUSY.getCode(), IdErrorCode.SERVER_BUSY.getMessage());
        }

        try {
            return (AllocResult) future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
        }

        try {
            // 同步等待结果（HTTP 阻塞）
            return (AllocResult) future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("[alloc] timeout, tag={}, count={}", tag, count);
            return AllocResult.fail(IdErrorCode.SERVER_BUSY.getCode(), IdErrorCode.SERVER_BUSY.getMessage());
        } catch (ExecutionException e) {
            log.error("[alloc] execution error", e.getCause());
            return AllocResult.fail(IdErrorCode.INTERNAL_ERROR.getCode(), e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AllocResult.fail(IdErrorCode.INTERNAL_ERROR.getCode(), "Thread interrupted");
        }
    }
}
