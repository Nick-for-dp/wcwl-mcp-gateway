package com.wcwl.mcpgateway.common.error;

import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.dto.response.McpErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局异常处理器
 * 
 * <p>这个类负责捕获整个应用中抛出的异常，并将它们转换为统一的错误响应格式。
 * 这样客户端总是能收到结构一致的错误信息。</p>
 * 
 * <h3>什么是 @ControllerAdvice？</h3>
 * <p>@ControllerAdvice 是 Spring MVC 的注解，用于定义全局的控制器增强。
 * 它可以用于：</p>
 * <ul>
 *   <li>全局异常处理（@ExceptionHandler）</li>
 *   <li>全局数据绑定（@InitBinder）</li>
 *   <li>全局模型属性（@ModelAttribute）</li>
 * </ul>
 * 
 * <h3>异常处理流程</h3>
 * <pre>
 * Controller 抛出异常
 *        ↓
 * Spring 查找匹配的 @ExceptionHandler
 *        ↓
 * 执行对应的处理方法
 *        ↓
 * 返回统一格式的错误响应
 * </pre>
 * 
 * <h3>为什么需要全局异常处理？</h3>
 * <ul>
 *   <li>统一错误响应格式 - 客户端只需处理一种错误结构</li>
 *   <li>集中管理 - 所有异常处理逻辑在一个地方</li>
 *   <li>避免重复 - 不需要在每个 Controller 中写 try-catch</li>
 *   <li>安全性 - 可以隐藏敏感的异常信息</li>
 * </ul>
 * 
 * @see McpToolException 自定义工具异常
 * @see ErrorCodes 错误码常量
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 MCP 工具异常
     * 
     * <p>McpToolException 是业务异常，包含错误码和状态码，
     * 可以直接转换为客户端友好的错误响应。</p>
     * 
     * <h3>@ExceptionHandler 注解</h3>
     * <p>指定这个方法处理哪种类型的异常。当 Controller 抛出
     * McpToolException 时，Spring 会调用这个方法。</p>
     * 
     * @param e 捕获的异常
     * @return 错误响应
     */
    @ExceptionHandler(McpToolException.class)
    public ResponseEntity<McpErrorResponse> handleMcpToolException(McpToolException e) {
        // 记录警告日志（不是错误，因为这是预期的业务异常）
        log.warn("MCP tool exception: code={}, message={}", e.getErrorCode(), e.getMessage());
        
        // 构建错误响应
        McpErrorResponse response = new McpErrorResponse(
                e.getErrorCode(),   // 错误码，如 "tool_not_found"
                e.getMessage(),     // 错误描述
                e.getStatusCode()   // HTTP 状态码
        );
        
        // 返回带有正确状态码的响应
        return ResponseEntity.status(e.getStatusCode()).body(response);
    }

    /**
     * 处理参数验证异常
     * 
     * <p>IllegalArgumentException 通常表示参数校验失败，
     * 返回 400 Bad Request。</p>
     * 
     * @param e 捕获的异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<McpErrorResponse> handleValidationException(IllegalArgumentException e) {
        log.warn("Validation exception: {}", e.getMessage());
        
        McpErrorResponse response = new McpErrorResponse(
                ErrorCodes.VALIDATION_ERROR,
                e.getMessage(),
                HttpStatus.BAD_REQUEST.value()  // 400
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理所有其他异常
     * 
     * <p>这是一个兜底处理器，捕获所有未被其他处理器处理的异常。
     * 返回通用的 500 Internal Server Error。</p>
     * 
     * <h3>安全注意事项</h3>
     * <p>不要将异常的详细信息返回给客户端，因为可能包含敏感信息
     * （如数据库连接字符串、堆栈跟踪等）。只返回通用的错误消息。</p>
     * 
     * @param e 捕获的异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<McpErrorResponse> handleException(Exception e) {
        // 记录错误日志，包含完整堆栈跟踪（用于调试）
        log.error("Unexpected exception: {}", e.getMessage(), e);
        
        // 返回通用错误消息，不暴露内部细节
        McpErrorResponse response = new McpErrorResponse(
                ErrorCodes.INTERNAL_ERROR,
                "Internal server error",  // 不返回 e.getMessage()
                HttpStatus.INTERNAL_SERVER_ERROR.value()  // 500
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
