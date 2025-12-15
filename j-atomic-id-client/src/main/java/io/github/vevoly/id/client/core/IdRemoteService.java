package io.github.vevoly.id.client.core;

import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.client.config.IdClientProperties;
import io.github.vevoly.id.client.utils.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <h3>远程 ID 申请服务 (Remote ID Allocation Service)</h3>
 *
 * <p>
 * 负责通过 HTTP 协议与 j-atomic-id-server 通信。
 * 内置了重试机制，确保在网络抖动时能尽可能获取到 ID。
 * </p>
 *
 * <hr>
 * <span style="color: gray; font-size: 0.9em;">
 * <b>Remote ID Allocation Service.</b><br>
 * Handles HTTP communication with j-atomic-id-server.<br>
 * Built-in retry mechanism to ensure availability during network jitters.
 * </span>
 *
 * @author vevoly
 */
@Slf4j
public class IdRemoteService {

    private final RestTemplate restTemplate;
    private final IdClientProperties properties;

    public IdRemoteService(RestTemplate restTemplate, IdClientProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 同步申请 ID 号段 (Synchronously Allocate ID Segment).
     *
     * @param bizTag 业务标识
     * @param step   申请数量
     * @return 申请结果 (包含 minId, maxId)
     */
    public AllocResult alloc(String bizTag, int step) {
        // 1. 构建 URL: http://localhost:8090/id/alloc?tag=order&count=1000
        String url = UriComponentsBuilder.fromHttpUrl(properties.getServerUrl())
                .path("/id/alloc")
                .queryParam("tag", bizTag)
                .queryParam("count", step)
                .toUriString();

        try {
            // 2. 执行请求 (带重试: 最多试3次，间隔200ms) / Retry: max 3 attempts, 200ms interval
            return Retryer.execute(() -> {
                // 发送 GET/POST 请求 (这里用 POST 更符合语义，虽然参数在 URL 上)
                AllocResult result = restTemplate.postForObject(url, null, AllocResult.class);

                // 校验业务结果
                if (result == null || !result.isSuccess()) {
                    throw new RuntimeException("Server returned error: " + (result != null ? result.getMessage() : "Empty Response"));
                }
                return result;
            }, 3, 200);

        } catch (Exception e) {
            // 3. 彻底失败，抛出运行时异常，中断业务
            log.error("[j-atomic-id-client] Failed to alloc ID for tag: {}, url: {}", bizTag, url, e);
            throw new RuntimeException("ID Allocation Failed after retries", e);
        }
    }
}
