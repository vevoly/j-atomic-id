package io.github.vevoly.id.server.service;

import io.github.vevoly.id.api.domain.AllocCommand;
import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import io.github.vevoly.id.server.domain.IdState;
import io.github.vevoly.id.server.mapper.IdGeneratorMapper;
import io.github.vevoly.ledger.api.LedgerBootstrap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class IdBootstrap implements LedgerBootstrap<IdState, AllocCommand> {

    @Autowired
    private IdGeneratorMapper idMapper;

    /**
     * 从数据库加载初始状态
     * 只有在本地没有快照文件时（冷启动）才会执行这里
     */
    @Override
    public IdState getInitialState() {
        log.info(">>> 检测到本地无快照，正在从数据库加载全量 ID 数据...");
        IdState state = new IdState();
        // 1. 查询所有业务 Tag (SELECT * FROM t_id_generator)
        // 如果 Tag 达到百万级，建议使用分页查询 (MyBatis-Plus Pagination) 循环加载。
        List<IdGeneratorEntity> allTags = idMapper.selectList(null);

        // 2. 将数据装入内存 Map
        // 数据库的一行记录 -> Map 的一个 Entry
        for (IdGeneratorEntity entity : allTags) {
            // Key: bizTag, Value: maxId
            state.getSequences().put(entity.getBizTag(), entity.getMaxId());
        }
        log.info("<<< 初始状态加载完成，共加载 {} 个业务 Tag。", allTags.size());

        // 返回填充好的状态对象
        return state;
    }

    @Override
    public Class<AllocCommand> getCommandClass() {
        return AllocCommand.class;
    }
}
