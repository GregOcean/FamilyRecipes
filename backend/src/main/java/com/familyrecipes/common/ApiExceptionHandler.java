package com.familyrecipes.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception error) {
        if (error instanceof org.springframework.web.ErrorResponse httpError) {
            int status = httpError.getStatusCode().value();
            return ResponseEntity.status(status).body(Result.error(status, "请求不可处理"));
        }
        log.error("Unhandled API failure", error);
        return ResponseEntity.internalServerError().body(Result.error(500, "请求处理失败"));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Result.error(400, "请求参数不合法"));
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> oversized(Exception error) {
        return ResponseEntity.status(413).body(Result.error(413, "上传文件过大"));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handle(ApiException error) {
        if (error.getStatus() >= 500) log.error("API failure", error);
        return ResponseEntity.status(error.getStatus()).body(Result.error(error.getStatus(), error.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> validation(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream()
            .map(org.springframework.validation.FieldError::getDefaultMessage).filter(java.util.Objects::nonNull)
            .findFirst().orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }
}
