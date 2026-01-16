package com.wcwl.mcpgateway.controller;

import com.wcwl.mcpgateway.dto.response.McpManifestResponse;
import com.wcwl.mcpgateway.dto.response.McpManifestResponse.ToolDefinition;
import com.wcwl.mcpgateway.service.tool.ToolRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MCP 工具清单控制器
 * 
 * <p>这个控制器提供工具清单（Manifest）接口，让客户端可以发现
 * 所有可用的工具及其参数定义。</p>
 * 
 * <h3>什么是工具清单？</h3>
 * <p>工具清单是一个 JSON 文档，列出了所有已注册的工具，包括：</p>
 * <ul>
 *   <li>工具名称 - 用于调用工具</li>
 *   <li>工具描述 - 说明工具功能</li>
 *   <li>输入参数 Schema - 定义需要传什么参数</li>
 * </ul>
 * 
 * <h3>API 使用示例</h3>
 * <pre>
 * GET /mcp/manifest
 * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 * 
 * 响应：
 * {
 *   "tools": [
 *     {
 *       "name": "get_warehouse_inventory",
 *       "description": "查询仓库库存",
 *       "inputSchema": {
 *         "type": "object",
 *         "properties": {
 *           "sku": {"type": "string", "description": "商品SKU编码"}
 *         },
 *         "required": ["sku"]
 *       }
 *     }
 *   ]
 * }
 * </pre>
 * 
 * @see ToolRegistryService 工具注册中心接口
 */
@RestController
@RequestMapping("/mcp")
public class ManifestController {

    /**
     * 工具注册中心服务
     * 
     * <p>依赖接口而非具体实现，实现依赖倒置原则。</p>
     */
    private final ToolRegistryService toolRegistry;

    /**
     * 构造函数注入
     * 
     * @param toolRegistry 工具注册中心服务实例
     */
    public ManifestController(ToolRegistryService toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 获取所有已注册工具的清单
     * 
     * <h3>@GetMapping 注解</h3>
     * <p>处理 HTTP GET 请求。完整路径是 /mcp/manifest
     * （类上的 /mcp + 方法上的 /manifest）。</p>
     * 
     * <h3>Stream API 说明</h3>
     * <p>这里使用了 Java 8 的 Stream API 来转换数据：</p>
     * <ol>
     *   <li>getAllTools() - 获取所有工具</li>
     *   <li>.stream() - 转换为 Stream</li>
     *   <li>.map() - 将每个 McpTool 转换为 ToolDefinition</li>
     *   <li>.toList() - 收集为 List</li>
     * </ol>
     * 
     * @return 包含所有工具定义的清单响应
     */
    @GetMapping("/manifest")
    public ResponseEntity<McpManifestResponse> getManifest() {
        // 只返回已发布的工具
        List<ToolDefinition> tools = toolRegistry.getPublishedTools().stream()
                // 将 McpTool 转换为 ToolDefinition
                // 只保留客户端需要的信息：名称、描述、参数 Schema
                .map(tool -> new ToolDefinition(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getInputSchema()
                ))
                // 收集为 List
                .toList();

        // 包装为响应对象并返回
        return ResponseEntity.ok(new McpManifestResponse(tools));
    }
}
