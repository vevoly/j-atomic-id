package io.github.vevoly.id.server.api;

import io.github.vevoly.id.api.domain.AllocCommand;
import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import io.github.vevoly.id.server.domain.IdState;
import io.github.vevoly.ledger.core.LedgerEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

        // 2. 提交
        engine.submit(cmd);

        // 3. 等待
        try {
            return (AllocResult) future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            return AllocResult.fail(e.getMessage());
        }
    }
}
