package com.hospital.req.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** ------------------- 自定义业务异常 ------------------- */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        log.warn("404 {} - {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBad(BadRequestException ex, HttpServletRequest req) {
        log.warn("400 {} - {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /** ------------------- 参数校验 ------------------- */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("400 {} - validation failed: {}", req.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(Map.of("message", "参数校验失败", "errors", errors));
    }

    /** ------------------- 上传相关 ------------------- */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("413 {} - file too large: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(413).body(Map.of("message", "文件过大，请压缩后再上传"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> handleMultipart(MultipartException ex, HttpServletRequest req) {
        log.warn("400 {} - multipart error: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", "上传表单格式不正确或请求被中断"));
    }

    /** ------------------- 404 (让 /error 正常触发) ------------------- */
    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    public ResponseEntity<?> handle404(Exception ex, HttpServletRequest req) {
        log.warn("404 {} - {}", req.getRequestURI(), ex.getMessage());
        // 不返回 500，而是保持 404 状态，让 SpaErrorController 接管
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /** ------------------- 兜底的 500 ------------------- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex, HttpServletRequest req) {
        // 注意：前面已经把 404 类型排除在外
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("500 {} [id={}] - {}", req.getRequestURI(), errorId, ex.toString(), ex);
        return ResponseEntity.status(500).body(Map.of(
                "message", "服务器内部错误",
                "errorId", errorId
        ));
    }
}
