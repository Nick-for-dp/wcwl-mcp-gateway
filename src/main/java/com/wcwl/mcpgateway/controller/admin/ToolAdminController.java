package com.wcwl.mcpgateway.controller.admin;

import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.dto.request.ToolRegisterRequest;
import com.wcwl.mcpgateway.model.mcp.ToolStatus;
import com.wcwl.mcpgateway.service.tool.ToolRegistryService;
import com.wcwl.mcpgateway.tools.DynamicProxyTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 工具管理控制器（仅 ADMIN 角色可访问）
 * 
 * <p>提供工具的动态注册和管理功能。第三方用户可以通过 API 注册自己的服务为 MCP 工具，
 * 无需修改代码。</p>
 * 
 * <h3>访问权限</h3>
 * <p>根据 SecurityConfig 的配置，/admin/** 路径需要 ADMIN 角色才能访问。</p>
 * 
 * <h3>注册工具示例</h3>
 * <pre>
 * POST /admin/tools/register
 * Authorization: Bearer &lt;admin-token&gt;
 * Content-Type: application/json
 * 
 * {
 *   "name": "query_trade_data",
 *   "description": "查询贸易数据",
 *   "endpoint": "http://api.example.com/trade/query",
 *   "method": "POST",
 *   "params": [
 *     {"name": "startDate", "type": "string", "required": true, "description": "开始日期"},
 *     {"name": "endDate", "type": "string", "required": true, "description": "结束日期"}
 *   ],
 *   "headers": {
 *     "X-API-Key": "your-api-key"
 *   }
 * }
 * </pre>
 * 
 * @see ToolRegistryService 工具注册中心接口
 * @see DynamicProxyTool 动态代理工具
 */
@RestController
@RequestMapping("/admin/tools")
public class ToolAdminController {

    private static final Logger log = LoggerFactory.getLogger(ToolAdminController.class);

    private final ToolRegistryService toolRegistry;
    private final RestTemplate restTemplate;

    public ToolAdminController(ToolRegistryService toolRegistry, RestTemplate restTemplate) {
        this.toolRegistry = toolRegistry;
        this.restTemplate = restTemplate;
    }

    /**
     * 注册动态工具
     * 
     * <p>接收工具配置，创建动态代理工具并注册到系统中。
     * 注册后，用户可以通过 /mcp/tools/{name} 调用该工具。</p>
     * 
     * <h3>工作流程</h3>
     * <ol>
     *   <li>验证请求参数</li>
     *   <li>检查工具名称是否已存在</li>
     *   <li>创建 DynamicProxyTool 实例</li>
     *   <li>注册到 ToolRegistry</li>
     * </ol>
     * 
     * @param request 工具配置
     * @param auth 当前用户认证信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerTool(
            @RequestBody ToolRegisterRequest request,
            Authentication auth) {
        // 参数校验
        validateRequest(request);

        // 检查工具是否已存在
        if (toolRegistry.hasTool(request.getName())) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM,
                    "Tool already exists: " + request.getName(), 409);
        }

        // 获取当前用户名
        String username = auth != null ? auth.getName() : "unknown";

        // 创建动态代理工具（传入用户名和分类）
        DynamicProxyTool tool = new DynamicProxyTool(request, restTemplate, username);

        // 注册工具
        toolRegistry.register(tool);

        log.info("Dynamic tool registered: name={}, endpoint={}, createdBy={}", 
                request.getName(), request.getEndpoint(), username);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tool registered successfully",
                "tool", Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "endpoint", request.getEndpoint(),
                        "category", request.getCategory() != null ? request.getCategory() : "custom",
                        "status", tool.getMetadata().getStatus().name(),
                        "createdBy", username
                )
        ));
    }

    /**
     * 注销工具
     * 
     * @param name 工具名称
     * @return 注销结果
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> unregisterTool(@PathVariable String name) {
        if (!toolRegistry.hasTool(name)) {
            throw new McpToolException(ErrorCodes.TOOL_NOT_FOUND,
                    "Tool not found: " + name, 404);
        }

        toolRegistry.unregister(name);

        log.info("Tool unregistered: {}", name);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Tool unregistered successfully",
                "toolName", name
        ));
    }

    /**
     * 获取已注册工具列表
     * 
     * @return 工具列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listTools() {
        var tools = toolRegistry.getAllTools().stream()
                .map(tool -> {
                    var metadata = tool.getMetadata();
                    return Map.of(
                            "name", tool.getName(),
                            "description", tool.getDescription(),
                            "category", metadata.getCategory(),
                            "status", metadata.getStatus().name(),
                            "statusDisplay", metadata.getStatus().getDisplayName(),
                            "sourceType", metadata.getSourceType().name(),
                            "createdBy", metadata.getCreatedBy(),
                            "createdAt", metadata.getCreatedAt().toString(),
                            "updatedBy", metadata.getUpdatedBy(),
                            "updatedAt", metadata.getUpdatedAt().toString()
                    );
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "count", tools.size(),
                "tools", tools
        ));
    }

    /**
     * 上架工具
     * 
     * @param name 工具名称
     * @param auth 当前用户认证信息
     * @return 操作结果
     */
    @PostMapping("/{name}/publish")
    public ResponseEntity<Map<String, Object>> publishTool(
            @PathVariable String name,
            Authentication auth) {
        if (!toolRegistry.hasTool(name)) {
            throw new McpToolException(ErrorCodes.TOOL_NOT_FOUND,
                    "Tool not found: " + name, 404);
        }

        String operator = auth != null ? auth.getName() : "unknown";
        boolean success = toolRegistry.updateToolStatus(name, ToolStatus.PUBLISHED, operator);

        if (success) {
            log.info("Tool published: name={}, operator={}", name, operator);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tool published successfully",
                    "toolName", name,
                    "status", ToolStatus.PUBLISHED.name()
            ));
        } else {
            throw new McpToolException(ErrorCodes.TOOL_EXECUTION_ERROR,
                    "Failed to publish tool", 500);
        }
    }

    /**
     * 下架工具
     * 
     * @param name 工具名称
     * @param auth 当前用户认证信息
     * @return 操作结果
     */
    @PostMapping("/{name}/offline")
    public ResponseEntity<Map<String, Object>> offlineTool(
            @PathVariable String name,
            Authentication auth) {
        if (!toolRegistry.hasTool(name)) {
            throw new McpToolException(ErrorCodes.TOOL_NOT_FOUND,
                    "Tool not found: " + name, 404);
        }

        String operator = auth != null ? auth.getName() : "unknown";
        boolean success = toolRegistry.updateToolStatus(name, ToolStatus.OFFLINE, operator);

        if (success) {
            log.info("Tool offline: name={}, operator={}", name, operator);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tool offline successfully",
                    "toolName", name,
                    "status", ToolStatus.OFFLINE.name()
            ));
        } else {
            throw new McpToolException(ErrorCodes.TOOL_EXECUTION_ERROR,
                    "Failed to offline tool", 500);
        }
    }

    /**
     * 验证注册请求
     */
    private void validateRequest(ToolRegisterRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM, 
                    "Tool name is required", 400);
        }

        if (request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM, 
                    "Endpoint URL is required", 400);
        }

        // 验证工具名称格式（只允许小写字母、数字、下划线）
        if (!request.getName().matches("^[a-z][a-z0-9_]*$")) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM,
                    "Tool name must start with lowercase letter and contain only lowercase letters, numbers, and underscores", 400);
        }

        // 验证 endpoint URL 格式
        if (!request.getEndpoint().startsWith("http://") && 
            !request.getEndpoint().startsWith("https://")) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM,
                    "Endpoint must be a valid HTTP/HTTPS URL", 400);
        }
    }
}
