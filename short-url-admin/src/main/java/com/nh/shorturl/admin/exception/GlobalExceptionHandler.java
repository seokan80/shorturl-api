package com.nh.shorturl.admin.exception;

import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * admin API 컨트롤러({@code com.nh.shorturl.admin.controller})에만 적용되는 공통 예외 핸들러.
 *
 * <p>컨트롤러가 자체 try-catch 없이 서비스 예외를 그대로 전파하면,
 * 여기서 기존 {@link ResultEntity}+{@link ApiResult} 응답 포맷으로 일관되게 매핑한다.
 *
 * <p>프론트엔드는 HTTP 상태 코드가 아니라 {@code code} 필드를 보고 분기하므로,
 * 현재 동작과의 호환을 위해 HTTP 상태는 200 으로 유지한다.
 *
 * <p>내부 API 컨트롤러({@code InternalApiController})도 같은 패키지에 있으므로 함께 적용된다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.nh.shorturl.admin.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResultEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResultEntity.of(ApiResult.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResultEntity<?> handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        return ResultEntity.of(ApiResult.FAIL);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResultEntity<?> handleBadRequest(Exception e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResultEntity.of(ApiResult.INVALID_PARAMETER);
    }

    @ExceptionHandler(Exception.class)
    public ResultEntity<?> handleFallback(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResultEntity.of(ApiResult.FAIL);
    }
}
