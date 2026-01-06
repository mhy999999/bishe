package com.bishe.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern MYSQL_DUPLICATE_ENTRY_PATTERN =
            Pattern.compile("Duplicate entry '([^']+)' for key '([^']+)'", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler({DuplicateKeyException.class, DataIntegrityViolationException.class})
    public Result<String> handleDataIntegrityViolation(RuntimeException e) {
        Throwable root = getRootCause(e);
        String message = root != null ? root.getMessage() : e.getMessage();
        String friendly = buildFriendlyDataIntegrityMessage(message);
        if (friendly != null) {
            log.warn("数据完整性异常: {}", friendly);
            return Result.error(400, friendly);
        }

        log.error("数据完整性异常", e);
        return Result.error(400, "数据保存失败，请检查数据是否重复或是否满足约束条件");
    }

    @ExceptionHandler(DataAccessException.class)
    public Result<String> handleDataAccessException(DataAccessException e) {
        Throwable root = getRootCause(e);
        String message = root != null ? root.getMessage() : e.getMessage();
        String friendly = buildFriendlyDataIntegrityMessage(message);
        if (friendly != null) {
            log.warn("数据库异常: {}", friendly);
            return Result.error(400, friendly);
        }

        log.error("数据库异常", e);
        return Result.error(500, "数据库操作失败，请稍后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制: {}", e.getMessage());
        return Result.error(400, "文件大小超过限制（单个≤10MB，总≤30MB）");
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "系统异常，请稍后重试";
        }
        return Result.error(500, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("业务异常", e);
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = "操作失败";
        }
        return Result.error(500, message);
    }

    private static Throwable getRootCause(Throwable t) {
        if (t == null) {
            return null;
        }
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String buildFriendlyDataIntegrityMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = MYSQL_DUPLICATE_ENTRY_PATTERN.matcher(message);
        if (matcher.find()) {
            String entry = matcher.group(1);
            String key = matcher.group(2);
            String entity = mapEntityByKey(key);
            if (entity != null) {
                return "保存失败：" + entity + "已存在（" + entry + "），请勿重复添加";
            }
            return "保存失败：数据已存在（" + entry + "），请勿重复提交";
        }
        return null;
    }

    private static String mapEntityByKey(String key) {
        if (key == null) {
            return null;
        }
        String lower = key.toLowerCase();
        if (lower.startsWith("battery_info.")) {
            return "电池ID";
        }
        if (lower.startsWith("battery_batch.")) {
            return "批次ID";
        }
        if (lower.startsWith("sys_user.")) {
            return "用户信息";
        }
        if (lower.startsWith("sys_role.")) {
            return "角色信息";
        }
        if (lower.startsWith("sys_dept.")) {
            return "部门信息";
        }
        if (lower.startsWith("sys_menu.")) {
            return "菜单信息";
        }
        if (lower.startsWith("sys_dict.")) {
            return "字典信息";
        }
        return null;
    }
}
