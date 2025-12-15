package io.github.vevoly.id.client.core.generator;

import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.client.config.IdClientProperties;
import io.github.vevoly.id.client.config.IdMode;
import io.github.vevoly.id.client.core.IdRemoteService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h3>智能 ID 生成器 (Smart ID Generator)</h3>
 *
 * <p>
 * 客户端调用的总入口。
 * 根据配置自动路由到 {@link SegmentBuffer} (号段) 或直接 RPC (严格)。
 * </p>
 */
@Slf4j
public class SmartIdGenerator implements IdGenerator {

    private final IdRemoteService remoteService;
    private final IdClientProperties properties;

    // 异步加载线程池 (所有号段共用)
    private final ExecutorService asyncLoader = Executors.newCachedThreadPool();

    // 缓存所有已初始化的号段 Buffer
    // Key: bizTag
    private final Map<String, SegmentBuffer> bufferMap = new ConcurrentHashMap<>();

    public SmartIdGenerator(IdRemoteService remoteService, IdClientProperties properties) {
        this.remoteService = remoteService;
        this.properties = properties;
    }

    @Override
    public long nextId(String bizTag) {
        // 1. 查找该 Tag 的配置规则
        IdClientProperties.Rule rule = findRule(bizTag);

        // 2. 严格模式 (IM 场景)：直接调远程，不缓冲
        if (rule.getMode() == IdMode.STRICT) {
            AllocResult result = remoteService.alloc(bizTag, 1);
            return result.getMinId();
        }

        // 3. 号段模式 (订单场景)：走双 Buffer
        SegmentBuffer buffer = bufferMap.computeIfAbsent(bizTag, k -> {
            // 懒加载初始化 Buffer
            return new SegmentBuffer(k, rule, remoteService, asyncLoader);
        });

        return buffer.nextId();
    }

    // 查找配置规则，如果没有配置，返回默认值
    private IdClientProperties.Rule findRule(String tag) {
        if (properties.getRules() != null) {
            for (IdClientProperties.Rule rule : properties.getRules()) {
                // 简单的精确匹配 (未来可支持正则匹配)
                if (rule.getBizTag().equals(tag)) {
                    return rule;
                }
            }
        }
        // 默认规则：号段模式，步长1000
        IdClientProperties.Rule defaultRule = new IdClientProperties.Rule();
        defaultRule.setBizTag(tag);
        return defaultRule;
    }
}
