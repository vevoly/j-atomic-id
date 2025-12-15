package io.github.vevoly.id.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.vevoly.id.server.domain.IdGeneratorEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IdGeneratorMapper extends BaseMapper<IdGeneratorEntity> {

    /**
     * 批量 Upsert (插入或更新)
     * 利用 MySQL 的 ON DUPLICATE KEY UPDATE 特性
     */
    void batchUpsert(@Param("list") List<IdGeneratorEntity> list);
}
