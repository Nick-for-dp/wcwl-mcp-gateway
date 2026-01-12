package com.wcwl.mcpgateway.common.exception;

import lombok.Getter;

/**
 * MCP 工具执行异常
 * 
 * <p>这是一个自定义的运行时异常，用于表示工具执行过程中的错误。
 * 它包含了错误码和 HTTP 状态码，便于统一的错误处理。</p>
 * 
 * <h3>为什么继承 RuntimeException？</h3>
 * <p>Java 异常分为两类：</p>
 * <ul>
 *   <li>Checked Exception（检查型异常）- 必须显式处理（try-catch 或 throws）</li>
 *   <li>Unchecked Exception（非检查型异常）- 不强制处理，RuntimeException 属于此类</li>
 * </ul>
 * <p>使用 RuntimeException 可以让代码更简洁，异常会自动向上传播，
 * 最终被 GlobalExceptionHandler 统一处理。</p>
 * 
 * <h3>@Getter 注解</h3>
 * <p>Lombok 注解，自动生成所有字段的 getter 方法。
 * 等价于手动编写 getErrorCode() 和 getStatusCode() 方法。</p>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 工具不存在
 * throw new McpToolException("tool_not_found", "Tool not found: xxx", 404);
 * 
 * // 权限不足
 * throw new McpToolException("forbidden", "Required roles: ADMIN", 403);
 * 
 * // 参数错误
 * throw new McpToolException("invalid_param", "sku is required", 400);
 * }</pre>
 * 
 * @see com.wcwl.mcpgateway.common.error.GlobalExceptionHandler 全局异常处理器
 */
@Getter
public class McpToolException extends RuntimeException {

    /**
     * 错误码
     * 
     * <p>用于标识错误类型的字符串，如：</p>
     * <ul>
     *   <li>"tool_not_found" - 工具不存在</li>
     *   <li>"unauthorized" - 未认证</li>
     *   <li>"forbidden" - 权限不足</li>
     *   <li>"invalid_param" - 参数错误</li>
     *   <li>"tool_execution_error" - 执行错误</li>
     * </ul>
     */
    private final String errorCode;

    /**
     * HTTP 状态码
     * 
     * <p>常用状态码：</p>
     * <ul>
     *   <li>400 - Bad Request（请求参数错误）</li>
     *   <li>401 - Unauthorized（未认证）</li>
     *   <li>403 - Forbidden（权限不足）</li>
     *   <li>404 - Not Found（资源不存在）</li>
     *   <li>500 - Internal Server Error（服务器内部错误）</li>
     * </ul>
     */
    private final int statusCode;

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message 错误描述（会传递给父类 RuntimeException）
     * @param statusCode HTTP 状态码
     */
    public McpToolException(String errorCode, String message, int statusCode) {
        // 调用父类构造函数，设置异常消息
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
}
