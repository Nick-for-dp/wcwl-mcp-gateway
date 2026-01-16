package com.wcwl.mcpgateway.service.tool;

import com.wcwl.mcpgateway.model.mcp.McpTool;
import com.wcwl.mcpgateway.model.mcp.ToolStatus;

import java.util.Collection;

/**
 * 工具注册中心服务接口
 * 
 * <p>定义工具注册中心的核心功能。通过接口抽象，实现依赖倒置原则（DIP）。</p>
 * 
 * <h3>什么是依赖倒置原则？</h3>
 * <p>依赖倒置原则（Dependency Inversion Principle）是 SOLID 原则之一：</p>
 * <ul>
 *   <li>高层模块不应该依赖低层模块，两者都应该依赖抽象</li>
 *   <li>抽象不应该依赖细节，细节应该依赖抽象</li>
 * </ul>
 * 
 * <h3>为什么需要这个接口？</h3>
 * <ul>
 *   <li>解耦 - Controller 依赖接口而非具体实现</li>
 *   <li>可测试 - 可以轻松创建 Mock 实现进行单元测试</li>
 *   <li>可扩展 - 可以有多种实现（如内存实现、数据库实现）</li>
 *   <li>清晰的契约 - 接口明确定义了服务提供的功能</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // Controller 依赖接口
 * public class McpToolController {
 *     private final ToolRegistryService toolRegistry;
 *     
 *     public McpToolController(ToolRegistryService toolRegistry) {
 *         this.toolRegistry = toolRegistry;
 *     }
 * }
 * }</pre>
 * 
 * @see ToolRegistry 默认实现
 */
public interface ToolRegistryService {

    /**
     * 注册工具
     * 
     * <p>将工具添加到注册中心。如果同名工具已存在，
     * 具体行为由实现类决定（可能覆盖或忽略）。</p>
     * 
     * @param tool 要注册的工具实例
     */
    void register(McpTool tool);

    /**
     * 根据名称获取工具
     * 
     * @param name 工具名称
     * @return 工具实例，如果不存在返回 null
     */
    McpTool getTool(String name);

    /**
     * 获取所有已注册的工具
     * 
     * @return 所有工具的集合（不可修改）
     */
    Collection<McpTool> getAllTools();

    /**
     * 检查工具是否存在
     * 
     * @param name 工具名称
     * @return true 如果工具存在，否则 false
     */
    boolean hasTool(String name);

    /**
     * 注销工具
     * 
     * <p>从注册中心移除指定工具。</p>
     * 
     * @param name 要注销的工具名称
     */
    void unregister(String name);

    /**
     * 获取所有已发布的工具（供普通用户使用）
     * 
     * @return 已发布状态的工具集合
     */
    Collection<McpTool> getPublishedTools();

    /**
     * 更新工具状态
     * 
     * @param name 工具名称
     * @param status 新状态
     * @param operator 操作人
     * @return 是否更新成功
     */
    boolean updateToolStatus(String name, ToolStatus status, String operator);
}
