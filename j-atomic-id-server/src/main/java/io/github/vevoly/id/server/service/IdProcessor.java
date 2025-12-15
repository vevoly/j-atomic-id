package io.github.vevoly.id.server.service;

import io.github.vevoly.id.api.domain.AllocCommand;
import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import io.github.vevoly.id.server.domain.IdState;
import io.github.vevoly.ledger.api.BusinessProcessor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class IdProcessor implements BusinessProcessor<IdState, AllocCommand, IdGeneratorEntity> {

    @Override
    public IdGeneratorEntity process(IdState state, AllocCommand cmd) {
        // 1. 获取申请数量
        int count = cmd.getCount();
        if (count <= 0) count = 1; // 默认给1个

        // 2. 获取当前 MaxId
        // 如果是新业务 Tag，默认从 0 开始
        long currentMax = state.getSequences().getOrDefault(cmd.getBizTag(), 0L);

        // 3. 计算新的 MaxId
        long nextMax = currentMax + count;

        // 4. 更新内存
        state.getSequences().put(cmd.getBizTag(), nextMax);

        // 5. 返回结果给 Controller
        if (cmd.getFuture() != null) {
            // 返回号段: (currentMax, nextMax]
            AllocResult result = AllocResult.success(currentMax + 1, nextMax);
            cmd.getFuture().complete(result);
        }

        // 6. 返回增量实体用于落库
        IdGeneratorEntity idGeneratorEntity = new IdGeneratorEntity();
        idGeneratorEntity.setBizTag(cmd.getBizTag());
        idGeneratorEntity.setMaxId(nextMax);
        idGeneratorEntity.setStep(count);
        idGeneratorEntity.setUpdateTime(new Date());
        return idGeneratorEntity;
    }
}
