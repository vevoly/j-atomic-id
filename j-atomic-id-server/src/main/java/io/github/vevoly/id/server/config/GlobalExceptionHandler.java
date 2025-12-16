package io.github.vevoly.id.server.config;

import io.github.vevoly.id.api.exceptions.IdException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 (Global Exception Handler)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IdException.class)
    public ResponseEntity<ErrorResponse> handleIdException(IdException e) {
        ErrorResponse response = new ErrorResponse(e.getCode(), e.getMessage());
        // 根据 code 决定 HTTP 状态码
        HttpStatus status = e.getCode() >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        if (e.getCode() == 401) status = HttpStatus.UNAUTHORIZED;
        return new ResponseEntity<>(response, status);
    }

    // 简单的错误响应体
    record ErrorResponse(int code, String message) {}
}
