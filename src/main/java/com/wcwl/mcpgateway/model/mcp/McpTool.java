package com.wcwl.mcpgateway.model.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Set;

/**
 * MCP 工具接口定义
 * 
 * <p>这是整个工具系统的核心接口。所有想要被 MCP Gateway 管理和执行的工具
 * 都必须实现这个接口。</p>
 * 
 * <h3>什么是接口（Interface）？</h3>
 * <p>接口就像一份"合同"，它定义了实现类必须提供哪些方法。
 * 任何实现这个接口的类都必须实现接口中定义的所有方法。</p>
 * 
 * <h3>为什么使用接口？</h3>
 * <ul>
 *   <li>统一规范 - 所有工具都有相同的方法签名，便于统一管理</li>
 *   <li>解耦合 - 调用方只依赖接口，不依赖具体实现</li>
 *   <li>可扩展 - 可以轻松添加新工具，只需实现这个接口</li>
 * </ul>
 * 
 * <h3>如何创建一个新工具？</h3>
 * <pre>{@code
 * // 方式1：直接实现接口（需要实现所有方法）
 * public class MyTool implements McpTool {
 *     // 实现所有方法...
 * }
 * 
 * // 方式2：继承 BaseTool（推荐，只需实现核心方法）
 * public class MyTool extends BaseTool {
 *     // 只需实现 getName(), getDescription(), getInputSchema(), doExecute()
 * }
 * }</pre>
 * 
 * @see BaseTool 工具基类，提供了通用功能的实现
 * @see com.wcwl.mcpgateway.tools.builtin.SampleWarehouseTool 示例工具实现
 */
public interface McpTool {

    /**
     * 获取工具的唯一名称
     * 
     * <p>这个名称用于：</p>
     * <ul>
     *   <li>在 ToolRegistry 中注册和查找工具</li>
     *   <li>在 API 调用时指定要执行的工具（/mcp/tools/{toolName}）</li>
     *   <li>在工具清单中展示</li>
     * </ul>
     * 
     * <p>命名建议：使用小写字母和下划线，如 "get_warehouse_inventory"</p>
     * 
     * @return 工具名称，必须唯一且不为空
     */
    String getName();

    /**
     * 获取工具的描述信息
     * 
     * <p>描述应该清晰说明工具的功能，帮助用户理解这个工具能做什么。
     * 这个描述会在工具清单（/mcp/manifest）中返回给客户端。</p>
     * 
     * @return 工具描述，如 "查询仓库库存"
     */
    String getDescription();

    /**
     * 获取输入参数的 JSON Schema 定义
     * 
     * <h3>什么是 JSON Schema？</h3>
     * <p>JSON Schema 是一种描述 JSON 数据结构的规范。它定义了：</p>
     * <ul>
     *   <li>参数有哪些字段</li>
     *   <li>每个字段的类型（string、integer、boolean 等）</li>
     *   <li>哪些字段是必填的</li>
     *   <li>字段的描述信息</li>
     * </ul>
     * 
     * <h3>示例</h3>
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {
     *     "sku": {
     *       "type": "string",
     *       "description": "商品SKU编码"
     *     }
     *   },
     *   "required": ["sku"]
     * }
     * }</pre>
     * 
     * <p>可以使用 {@link JsonObjectSchema} 工具类来方便地构建 Schema。</p>
     * 
     * @return JSON Schema 定义
     * @see JsonObjectSchema 用于构建 Schema 的工具类
     */
    JsonNode getInputSchema();

    /**
     * 获取执行此工具所需的角色
     * 
     * <h3>什么是角色（Role）？</h3>
     * <p>角色是一种权限分组机制。用户可以拥有一个或多个角色，
     * 每个角色代表一组权限。常见角色如：</p>
     * <ul>
     *   <li>ADMIN - 管理员，拥有所有权限</li>
     *   <li>USER - 普通用户</li>
     *   <li>WAREHOUSE_VIEWER - 仓库查看者</li>
     * </ul>
     * 
     * <h3>default 关键字</h3>
     * <p>这是 Java 8 引入的接口默认方法。实现类可以选择覆盖这个方法，
     * 也可以直接使用默认实现（返回空集合，表示无角色限制）。</p>
     * 
     * @return 所需角色集合，空集合表示任何已认证用户都可以访问
     */
    default Set<String> getRequiredRoles() {
        return Set.of();  // Set.of() 创建一个不可变的空集合
    }

    /**
     * 获取工具元数据
     * 
     * <p>元数据包含工具的上传人、上传时间、分类、状态等信息。</p>
     * 
     * @return 工具元数据，默认返回内置工具的默认元数据
     */
    default ToolMetadata getMetadata() {
        return ToolMetadata.builtinDefault();
    }

    /**
     * 设置工具元数据
     * 
     * <p>用于更新工具的元数据信息，如状态变更等。</p>
     * 
     * @param metadata 新的元数据
     */
    default void setMetadata(ToolMetadata metadata) {
        // 默认空实现，子类可覆盖
    }

    /**
     * 执行工具
     * 
     * <p>这是工具的核心方法，包含实际的业务逻辑。</p>
     * 
     * <h3>参数说明</h3>
     * <ul>
     *   <li>args - 客户端传入的参数，是一个 Map 结构，key 是参数名，value 是参数值</li>
     *   <li>auth - Spring Security 的认证对象，包含当前用户信息和权限</li>
     * </ul>
     * 
     * <h3>返回值</h3>
     * <p>返回 JsonNode 类型，这是 Jackson 库中表示 JSON 的通用类型，
     * 可以表示任意 JSON 结构（对象、数组、字符串等）。</p>
     * 
     * @param args 输入参数，如 {"sku": "SKU001"}
     * @param auth 当前用户的认证信息，可以从中获取用户名、角色等
     * @return 执行结果，会被包装在 McpSuccessResponse 中返回给客户端
     * @throws McpToolException 当执行失败时抛出，会被全局异常处理器捕获
     * @see com.wcwl.mcpgateway.common.exception.McpToolException 工具异常类
     */
    JsonNode execute(Map<String, Object> args, Authentication auth);
}
