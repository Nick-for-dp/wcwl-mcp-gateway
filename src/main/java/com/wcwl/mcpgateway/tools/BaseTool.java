package com.wcwl.mcpgateway.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.common.logging.McpToolLogger;
import com.wcwl.mcpgateway.model.mcp.McpTool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP 工具抽象基类
 * 
 * <p>这是所有 MCP 工具的推荐基类。它使用了<b>模板方法设计模式</b>，
 * 将通用的处理逻辑（日志、权限、异常）封装在基类中，
 * 子类只需要关注核心业务逻辑。</p>
 * 
 * <h3>什么是抽象类（Abstract Class）？</h3>
 * <p>抽象类是一种不能被直接实例化的类，它通常包含：</p>
 * <ul>
 *   <li>已实现的方法 - 提供通用功能，子类可以直接使用</li>
 *   <li>抽象方法 - 只有方法签名，没有实现，子类必须实现</li>
 * </ul>
 * 
 * <h3>模板方法模式</h3>
 * <p>execute() 方法定义了工具执行的"骨架"流程：</p>
 * <pre>
 * 1. 记录调用日志
 * 2. 检查权限
 * 3. 执行具体逻辑（由子类实现）
 * 4. 记录结果日志
 * 5. 处理异常
 * </pre>
 * 
 * <h3>如何创建新工具？</h3>
 * <pre>{@code
 * @Component  // 标记为 Spring 组件，会被自动注册
 * public class MyTool extends BaseTool {
 *     
 *     @Override
 *     public String getName() {
 *         return "my_tool";
 *     }
 *     
 *     @Override
 *     public String getDescription() {
 *         return "我的工具描述";
 *     }
 *     
 *     @Override
 *     public JsonNode getInputSchema() {
 *         return new JsonObjectSchema()
 *             .addStringProperty("param1", "参数1描述")
 *             .setRequired(List.of("param1"))
 *             .toJsonNode();
 *     }
 *     
 *     @Override
 *     protected JsonNode doExecute(Map<String, Object> args, Authentication auth) {
 *         // 你的业务逻辑
 *         String param1 = (String) args.get("param1");
 *         return MAPPER.valueToTree(Map.of("result", "处理结果"));
 *     }
 * }
 * }</pre>
 * 
 * @see McpTool 工具接口定义
 * @see com.wcwl.mcpgateway.tools.builtin.SampleWarehouseTool 示例实现
 */
public abstract class BaseTool implements McpTool {

    /**
     * 执行工具的入口方法（模板方法）
     * 
     * <p>这个方法被标记为 final，意味着子类不能覆盖它。
     * 这确保了所有工具都遵循相同的执行流程。</p>
     * 
     * <h3>执行流程</h3>
     * <ol>
     *   <li>记录调用日志（谁调用了什么工具，传了什么参数）</li>
     *   <li>检查用户是否有权限执行此工具</li>
     *   <li>调用 doExecute() 执行具体业务逻辑</li>
     *   <li>记录执行耗时</li>
     *   <li>捕获并处理异常</li>
     * </ol>
     * 
     * @param args 客户端传入的参数
     * @param auth 当前用户的认证信息
     * @return 执行结果
     * @throws McpToolException 当权限不足或执行失败时抛出
     */
    @Override
    public final JsonNode execute(Map<String, Object> args, Authentication auth) {
        String toolName = getName();
        String userId = auth != null ? auth.getName() : "anonymous";

        // 记录调用日志
        McpToolLogger.logInvocation(toolName, userId, args);

        // 权限校验
        checkPermission(auth);

        long startTime = System.currentTimeMillis();
        try {
            // 执行具体逻辑
            JsonNode result = doExecute(args, auth);

            // 记录成功日志
            long duration = System.currentTimeMillis() - startTime;
            McpToolLogger.logSuccess(toolName, duration);

            return result;
        } catch (McpToolException e) {
            // MCP工具异常直接抛出
            McpToolLogger.logError(toolName, e);
            throw e;
        } catch (Exception e) {
            // 包装其他异常
            McpToolLogger.logError(toolName, e);
            throw new McpToolException(ErrorCodes.TOOL_EXECUTION_ERROR, e.getMessage(), 500);
        }
    }

    /**
     * 具体执行逻辑（由子类实现）
     * 
     * <p>这是一个抽象方法，子类必须实现它来提供具体的业务逻辑。</p>
     * 
     * <h3>实现建议</h3>
     * <ul>
     *   <li>从 args 中获取参数时，注意类型转换和空值检查</li>
     *   <li>使用 ObjectMapper 将结果转换为 JsonNode</li>
     *   <li>业务异常可以抛出 McpToolException，会被正确处理</li>
     * </ul>
     * 
     * @param args 输入参数，key 是参数名，value 是参数值
     * @param auth 认证信息，可以获取当前用户名：auth.getName()
     * @return 执行结果，会被包装在响应中返回
     */
    protected abstract JsonNode doExecute(Map<String, Object> args, Authentication auth);

    /**
     * 权限校验
     * 
     * <p>检查当前用户是否拥有执行此工具所需的角色。</p>
     * 
     * <h3>校验逻辑</h3>
     * <ol>
     *   <li>如果工具没有设置角色要求（requiredRoles 为空），直接通过</li>
     *   <li>如果用户未认证（auth 为 null），返回 401 Unauthorized</li>
     *   <li>获取用户的所有角色，检查是否包含任一所需角色</li>
     *   <li>如果没有匹配的角色，返回 403 Forbidden</li>
     * </ol>
     * 
     * <h3>角色格式说明</h3>
     * <p>Spring Security 的角色通常带有 "ROLE_" 前缀，如 "ROLE_ADMIN"。
     * 这里会自动去除前缀进行比较，所以工具定义角色时不需要加前缀。</p>
     * 
     * @param auth 当前用户的认证信息
     * @throws McpToolException 当权限不足时抛出
     */
    private void checkPermission(Authentication auth) {
        Set<String> requiredRoles = getRequiredRoles();
        if (requiredRoles.isEmpty()) {
            return;
        }

        if (auth == null) {
            throw new McpToolException(ErrorCodes.UNAUTHORIZED, "Authentication required", 401);
        }

        Set<String> userRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toSet());

        boolean hasPermission = requiredRoles.stream().anyMatch(userRoles::contains);
        if (!hasPermission) {
            throw new McpToolException(ErrorCodes.FORBIDDEN, 
                    "Required roles: " + requiredRoles, 403);
        }
    }
}
