package com.wcwl.mcpgateway.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * MCP工具日志工具类
 */
public final class McpToolLogger {

    private static final Logger log = LoggerFactory.getLogger(McpToolLogger.class);

    private McpToolLogger() {
        // 私有构造函数，防止实例化
    }

    /**
     * 记录工具调用
     */
    public static void logInvocation(String toolName, String userId, Map<String, Object> args) {
        log.info("[MCP] Tool invoked: tool={}, user={}, args={}", toolName, userId, args);
    }

    /**
     * 记录工具执行成功
     */
    public static void logSuccess(String toolName, long durationMs) {
        log.info("[MCP] Tool success: tool={}, duration={}ms", toolName, durationMs);
    }

    /**
     * 记录工具执行错误
     */
    public static void logError(String toolName, Exception e) {
        log.error("[MCP] Tool error: tool={}, error={}", toolName, e.getMessage(), e);
    }
}
