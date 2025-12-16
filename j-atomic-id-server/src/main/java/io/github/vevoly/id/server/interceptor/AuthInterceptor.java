package io.github.vevoly.id.server.interceptor;

import io.github.vevoly.id.api.exceptions.IdErrorCode;
import io.github.vevoly.id.api.exceptions.IdException;
import io.github.vevoly.id.api.utils.SignatureUtils;
import io.github.vevoly.id.server.config.IdServerProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 鉴权拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private IdServerProperties authProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        // ip白名单检查
        if (!checkIpWhitelist(request)) {
            throw new IdException(IdErrorCode.IP_NOT_ALLOWED.getCode(), IdErrorCode.IP_NOT_ALLOWED.getMessage());
        }

        // 如果禁用了鉴权，直接放行
        if (!authProperties.getAuth().isEnabled()) {
            return true;
        }
        // 1. 从 Header 获取认证信息
        String appKey = request.getHeader("X-App-Key");
        String timestamp = request.getHeader("X-Timestamp");
        String signature = request.getHeader("X-Signature");
        // 2. 基础校验
        if (appKey == null || timestamp == null || signature == null) {
            throw new IdException(IdErrorCode.MISSING_AUTH_HEADER.getCode(), IdErrorCode.MISSING_AUTH_HEADER.getMessage());
        }
        // 3. 时间戳校验 (防止重放攻击，例如只允许5分钟内的请求)
        try {
            long requestTime = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            if (Math.abs(now - requestTime) > TimeUnit.MINUTES.toMillis(5)) {
                throw new IdException(IdErrorCode.TIMESTAMP_EXPIRED.getCode(), IdErrorCode.TIMESTAMP_EXPIRED.getMessage());
            }
        } catch (NumberFormatException e) {
            throw new IdException(IdErrorCode.INVALID_PARAMS.getCode(), IdErrorCode.INVALID_PARAMS.getMessage());
        }
        // 4. 密钥校验
        String secret = authProperties.getAuth().getClients().get(appKey);
        if (secret == null) {
            throw new IdException(IdErrorCode.INVALID_PARAMS.getCode(), IdErrorCode.INVALID_PARAMS.getMessage());
        }
        // 5. 签名校验
        Map<String, String> paramsToSign = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) {
                paramsToSign.put(k, v[0]);
            }
        });
        if (!SignatureUtils.verify(signature, secret, paramsToSign)) {
            throw new IdException(IdErrorCode.SIGNATURE_INVALID.getCode(), IdErrorCode.SIGNATURE_INVALID.getMessage());
        }
        // 全部通过，放行
        return true;
    }

    private boolean checkIpWhitelist(HttpServletRequest request) {
        List<String> whiteList = authProperties.getAuth().getIpWhitelist();
        if (whiteList == null || whiteList.isEmpty()) {
            return true; // 没配白名单，默认放行
        }

        String ip = getClientIp(request);

        // CIDR 匹配 (高级) 或者 简单 String 匹配
        return whiteList.contains(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // XFF 可能包含多个 IP，取第一个
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip;
        }

        // 如果上面都没有，最后才用 getRemoteAddr() 兜底
        // 这通常适用于开发环境，或者应用直接暴露在公网
        ip = request.getRemoteAddr();
        return ip;
    }

    // 辅助方法：检查 IP 是否有效（非空、非 "unknown"）
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}
