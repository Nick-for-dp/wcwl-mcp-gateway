package com.wcwl.mcpgateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.dto.response.McpSuccessResponse;
import com.wcwl.mcpgateway.model.mcp.McpTool;
import com.wcwl.mcpgateway.model.mcp.ToolStatus;
import com.wcwl.mcpgateway.service.tool.ToolRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MCP 工具调用控制器
 * 
 * <p>这是处理工具执行请求的核心控制器。客户端通过这个接口调用已注册的工具。</p>
 * 
 * <h3>什么是 @RestController？</h3>
 * <p>@RestController = @Controller + @ResponseBody</p>
 * <ul>
 *   <li>@Controller - 标记这是一个 Spring MVC 控制器</li>
 *   <li>@ResponseBody - 方法返回值直接作为 HTTP 响应体（自动转为 JSON）</li>
 * </ul>
 * 
 * <h3>什么是 @RequestMapping？</h3>
 * <p>定义这个控制器处理的 URL 前缀。所有方法的路径都会加上这个前缀。
 * 例如：/mcp/tools + /{toolName} = /mcp/tools/{toolName}</p>
 * 
 * <h3>API 使用示例</h3>
 * <pre>
 * POST /mcp/tools/get_warehouse_inventory
 * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 * Content-Type: application/json
 * 
 * {
 *   "sku": "SKU001"
 * }
 * </pre>
 * 
 * @see ToolRegistryService 工具注册中心接口
 */
@RestController
@RequestMapping("/mcp/tools")
public class McpToolController {

    /**
     * 工具注册中心服务
     * 
     * <p>通过构造函数注入接口而非具体实现，实现依赖倒置。
     * Spring 会自动找到 ToolRegistryService 的实现类（ToolRegistry）并注入。</p>
     */
    private final ToolRegistryService toolRegistry;

    /**
     * 构造函数注入
     * 
     * <p>Spring 推荐使用构造函数注入而不是 @Autowired 字段注入，因为：</p>
     * <ul>
     *   <li>依赖关系更明确</li>
     *   <li>便于单元测试</li>
     *   <li>可以声明为 final，保证不可变性</li>
     * </ul>
     * 
     * @param toolRegistry 工具注册中心服务实例
     */
    public McpToolController(ToolRegistryService toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行 MCP 工具
     * 
     * <h3>注解说明</h3>
     * <ul>
     *   <li>@PostMapping("/{toolName}") - 处理 POST 请求，{toolName} 是路径变量</li>
     *   <li>@PathVariable - 从 URL 路径中提取变量值</li>
     *   <li>@RequestBody - 将请求体的 JSON 自动转换为 Map</li>
     *   <li>Authentication - Spring Security 自动注入当前认证用户信息</li>
     * </ul>
     * 
     * <h3>执行流程</h3>
     * <ol>
     *   <li>从 URL 中提取工具名称</li>
     *   <li>从 ToolRegistry 中查找工具</li>
     *   <li>如果工具不存在，抛出 404 异常</li>
     *   <li>调用工具的 execute 方法</li>
     *   <li>将结果包装在 McpSuccessResponse 中返回</li>
     * </ol>
     * 
     * @param toolName 工具名称，从 URL 路径中获取
     * @param arguments 工具参数，从请求体 JSON 中解析
     * @param authentication 当前用户认证信息，由 Spring Security 自动注入
     * @return 包含执行结果的响应
     * @throws McpToolException 当工具不存在或执行失败时抛出
     */
    @PostMapping("/{toolName}")
    public ResponseEntity<McpSuccessResponse> executeTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments,
            Authentication authentication) {

        // 从注册中心获取工具
        McpTool tool = toolRegistry.getTool(toolName);
        
        // 工具不存在时抛出异常，会被 GlobalExceptionHandler 捕获处理
        if (tool == null) {
            throw new McpToolException(ErrorCodes.TOOL_NOT_FOUND, 
                    "Tool not found: " + toolName, 404);
        }

        // 检查工具是否已发布
        if (tool.getMetadata().getStatus() != ToolStatus.PUBLISHED) {
            throw new McpToolException(ErrorCodes.FORBIDDEN,
                    "Tool is not available: " + toolName, 403);
        }

        // 执行工具并获取结果
        // tool.execute() 内部会进行权限校验、日志记录等
        JsonNode result = tool.execute(arguments, authentication);

        // 包装结果并返回
        // ResponseEntity.ok() 创建一个 HTTP 200 响应
        return ResponseEntity.ok(new McpSuccessResponse(result));
    }
}
