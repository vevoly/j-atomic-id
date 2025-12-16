package io.github.vevoly.id.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vevoly.id.api.domain.AllocResult;
import io.github.vevoly.id.api.exceptions.IdErrorCode;
import io.github.vevoly.id.api.utils.SignatureUtils;
import io.github.vevoly.id.client.config.IdClientProperties;
import io.github.vevoly.id.client.utils.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.ErrorResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // 1. 准备待签名参数
        Map<String, String> params = new HashMap<>();
        params.put("tag", bizTag);
        params.put("count", String.valueOf(step));
        // 2. 构建签名
        String appKey = properties.getAppKey();
        String secret = properties.getAppSecret();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = SignatureUtils.sign(secret, params);
        // 3. 构建 URL: http://localhost:8090/id/alloc?tag=order&count=1000
        String url = UriComponentsBuilder.fromHttpUrl(properties.getServerUrl())
                .path("/id/alloc")
                .queryParam("tag", bizTag)
                .queryParam("count", step)
                .toUriString();
        // 4. 构建 Header
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-App-Key", appKey);
        headers.set("X-Timestamp", timestamp);
        headers.set("X-Signature", signature);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // 5. 执行请求 (带重试: 最多试3次，间隔200ms) / Retry: max 3 attempts, 200ms interval
            return Retryer.execute(() -> {
                AllocResult result =  restTemplate.exchange(url, HttpMethod.POST, entity, AllocResult.class).getBody();
                // 校验业务结果
                if (result == null || !result.isSuccess()) {
                    throw new RuntimeException("Server returned error: " + (result != null ? result.getMessage() : "Empty Response"));
                }
                return result;
            }, 3, 200);

        } catch (HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            // 尝试解析服务端返回的错误 JSON
            try {
                log.warn("[j-atomic-id-client] Server returned error: {}", body);
                AllocResult allocResult = objectMapper.readValue(body, AllocResult.class);
                return AllocResult.fail(allocResult.getCode(), allocResult.getMessage());
            } catch (Exception parseEx) {
                // 如果 JSON 解析失败 (比如 Nginx 返回了 HTML)，返回通用的 HTTP 错误
                log.warn("[j-atomic-id-client] Failed to parse error body: {}", body, parseEx);
                return AllocResult.fail(e.getStatusCode().value(), "HTTP Error: " + e.getStatusText());
            }
        } catch (Exception e) {
            // 6. 彻底失败，抛出运行时异常，中断业务
            log.error("[j-atomic-id-client] Failed to alloc ID for tag: {}, url: {}", bizTag, url, e);
            return AllocResult.fail(IdErrorCode.SERVER_BUSY.getCode(), IdErrorCode.SERVER_BUSY.getMessage());
        }
    }
}
