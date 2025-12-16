package io.github.vevoly.id.api.exceptions;

/**
 * ID 服务通用异常基类
 */
public class IdException extends RuntimeException {
    private final int code;

    public IdException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
