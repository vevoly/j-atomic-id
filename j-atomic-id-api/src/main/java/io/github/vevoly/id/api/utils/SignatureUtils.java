package io.github.vevoly.id.api.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具类
 */
public class SignatureUtils {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 服务端校验签名
     */
    public static boolean verify(String signature, String secret, Map<String, String> params) {
        if (signature == null || secret == null || params == null) {
            return false;
        }
        String serverSignature = sign(secret, params);
        return signature.equals(serverSignature);
    }

    /**
     * 计算签名
     */
    public static String sign(String secret, Map<String, String> params) {
        // 1. 使用 TreeMap 保证参数按 Key 字典序排序
        Map<String, String> sortedParams = new TreeMap<>(params);

        // 2. 拼接字符串: key1=value1&key2=value2
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }

        // 3. HMAC-SHA256 加密
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));

            // 4. Base64 编码
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
}
