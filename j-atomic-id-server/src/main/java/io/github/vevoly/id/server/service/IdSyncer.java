package io.github.vevoly.id.server.service;

import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import io.github.vevoly.id.server.mapper.IdGeneratorMapper;
import io.github.vevoly.ledger.api.BatchWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdSyncer implements BatchWriter<IdGeneratorEntity> {

    @Autowired
    private IdGeneratorMapper idMapper;

    @Override
    public void persist(List<IdGeneratorEntity> entities) {
        idMapper.batchUpsert(entities);
    }
}
