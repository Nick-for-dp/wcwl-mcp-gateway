package com.wcwl.mcpgateway.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wcwl.mcpgateway.model.mcp.JsonObjectSchema;
import com.wcwl.mcpgateway.tools.BaseTool;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 示例工具：查询仓库库存
 * 
 * <p>这是一个完整的工具实现示例，展示了如何创建一个 MCP 工具。
 * 你可以参考这个类来创建自己的工具。</p>
 * 
 * <h3>创建工具的步骤</h3>
 * <ol>
 *   <li>继承 BaseTool 类</li>
 *   <li>添加 @Component 注解（让 Spring 自动注册）</li>
 *   <li>实现 getName() - 返回工具唯一名称</li>
 *   <li>实现 getDescription() - 返回工具描述</li>
 *   <li>实现 getInputSchema() - 定义输入参数结构</li>
 *   <li>实现 doExecute() - 编写业务逻辑</li>
 *   <li>（可选）覆盖 getRequiredRoles() - 设置权限要求</li>
 * </ol>
 * 
 * <h3>@Component 注解</h3>
 * <p>标记这个类是一个 Spring 组件。Spring 启动时会：</p>
 * <ol>
 *   <li>扫描到这个类</li>
 *   <li>创建它的实例</li>
 *   <li>将实例注入到 ToolRegistry 的构造函数</li>
 *   <li>ToolRegistry 自动注册这个工具</li>
 * </ol>
 * 
 * @see BaseTool 工具基类
 */
@Component
public class SampleWarehouseTool extends BaseTool {

    /**
     * Jackson ObjectMapper 实例
     * 
     * <p>用于将 Java 对象转换为 JSON。声明为 static final 是因为：</p>
     * <ul>
     *   <li>ObjectMapper 是线程安全的，可以共享使用</li>
     *   <li>创建 ObjectMapper 有一定开销，复用可以提高性能</li>
     * </ul>
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 返回工具名称
     * 
     * <p>这个名称用于 API 调用：POST /mcp/tools/get_warehouse_inventory</p>
     * 
     * @return 工具名称
     */
    @Override
    public String getName() {
        return "get_warehouse_inventory";
    }

    /**
     * 返回工具描述
     * 
     * <p>描述会在工具清单中展示，帮助用户了解工具功能。</p>
     * 
     * @return 工具描述
     */
    @Override
    public String getDescription() {
        return "查询仓库库存";
    }

    /**
     * 定义输入参数的 JSON Schema
     * 
     * <p>使用 JsonObjectSchema 工具类构建 Schema。
     * 这个 Schema 会在工具清单中返回，告诉客户端需要传什么参数。</p>
     * 
     * <h3>生成的 Schema 结构</h3>
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
     * @return JSON Schema
     */
    @Override
    public JsonNode getInputSchema() {
        return new JsonObjectSchema()
                // 添加一个字符串类型的参数 "sku"
                .addStringProperty("sku", "商品SKU编码")
                // 设置 "sku" 为必填参数
                .setRequired(List.of("sku"))
                // 转换为 JsonNode
                .toJsonNode();
    }

    /**
     * 获取执行此工具所需的角色
     * 
     * <p>返回空集合表示任何已认证用户都可以访问。
     * 如果需要限制访问，可以返回角色集合，如：</p>
     * <pre>{@code
     * return Set.of("WAREHOUSE_VIEWER", "ADMIN");
     * }</pre>
     * 
     * @return 所需角色集合
     */
    @Override
    public Set<String> getRequiredRoles() {
        // 预留角色限制，暂时返回空集合允许所有认证用户访问
        // 如果需要限制，取消下面的注释：
        // return Set.of("WAREHOUSE_VIEWER");
        return Set.of();
    }

    /**
     * 执行工具的核心业务逻辑
     * 
     * <p>这个方法由 BaseTool.execute() 调用，已经完成了日志记录和权限校验。
     * 你只需要专注于业务逻辑。</p>
     * 
     * <h3>参数获取</h3>
     * <p>从 args Map 中获取参数时，需要进行类型转换。
     * 常见类型：String、Integer、Boolean、List、Map</p>
     * 
     * <h3>返回值</h3>
     * <p>使用 ObjectMapper.valueToTree() 将 Java 对象转换为 JsonNode。
     * 支持 Map、List、POJO 等各种类型。</p>
     * 
     * @param args 输入参数，如 {"sku": "SKU001"}
     * @param auth 当前用户认证信息
     * @return 执行结果
     */
    @Override
    protected JsonNode doExecute(Map<String, Object> args, Authentication auth) {
        // ========== 1. 获取输入参数 ==========
        // 从 args 中获取 sku 参数，需要强制类型转换
        String sku = (String) args.get("sku");

        // ========== 2. 执行业务逻辑 ==========
        // 这里使用 Mock 数据模拟数据库查询
        // 实际项目中，这里应该调用数据库或其他服务
        List<Map<String, Object>> inventory = List.of(
                // 第一个仓库的库存信息
                Map.of(
                        "sku", sku,
                        "warehouseId", "WH001",
                        "warehouseName", "北京仓",
                        "quantity", 150,           // 总库存
                        "availableQuantity", 120   // 可用库存
                ),
                // 第二个仓库的库存信息
                Map.of(
                        "sku", sku,
                        "warehouseId", "WH002",
                        "warehouseName", "上海仓",
                        "quantity", 200,
                        "availableQuantity", 180
                )
        );

        // ========== 3. 构建返回结果 ==========
        // 使用 Map.of() 创建不可变 Map
        // MAPPER.valueToTree() 将 Map 转换为 JsonNode
        return MAPPER.valueToTree(Map.of(
                "sku", sku,
                "totalQuantity", 350,      // 总库存数量
                "warehouses", inventory    // 各仓库详情
        ));
    }
}
