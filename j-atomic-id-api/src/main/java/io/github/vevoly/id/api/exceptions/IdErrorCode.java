package io.github.vevoly.id.api.exceptions;

/**
 * 错误码枚举
 */
public enum IdErrorCode {
    AUTH_FAILED(4001, "Authentication failed"),
    SIGNATURE_INVALID(4002, "Signature verification failed"),
    TIMESTAMP_EXPIRED(4003, "Request timestamp expired"),
    IP_NOT_ALLOWED(4004, "IP address not in whitelist"),
    MISSING_AUTH_HEADER(4005, "Missing authentication header"),
    INVALID_PARAMS(4006, "Invalid parameters"),
    SERVER_BUSY(503, "Server is busy, please retry"),
    INTERNAL_ERROR(500, "Internal server error"),
    DUPLICATE_REQUEST(1001, "Duplicate request, idempotency check failed");

    private final int code;
    private final String message;

    IdErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
