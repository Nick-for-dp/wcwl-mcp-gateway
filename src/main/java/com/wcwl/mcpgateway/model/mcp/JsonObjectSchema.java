package com.wcwl.mcpgateway.model.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.util.List;

/**
 * JSON Schema 构建工具类
 * 
 * <p>这个类提供了一种流畅的方式来构建 JSON Schema。
 * JSON Schema 用于描述工具的输入参数结构。</p>
 * 
 * <h3>什么是 JSON Schema？</h3>
 * <p>JSON Schema 是一种描述 JSON 数据结构的标准。它定义了：</p>
 * <ul>
 *   <li>数据类型（string、integer、boolean、object、array）</li>
 *   <li>必填字段</li>
 *   <li>字段描述</li>
 *   <li>数据约束（最大值、最小值、正则表达式等）</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * JsonNode schema = new JsonObjectSchema()
 *     .addStringProperty("name", "用户名")
 *     .addIntegerProperty("age", "年龄")
 *     .addBooleanProperty("active", "是否激活")
 *     .setRequired(List.of("name", "age"))
 *     .toJsonNode();
 * }</pre>
 * 
 * <h3>生成的 Schema</h3>
 * <pre>{@code
 * {
 *   "type": "object",
 *   "properties": {
 *     "name": {"type": "string", "description": "用户名"},
 *     "age": {"type": "integer", "description": "年龄"},
 *     "active": {"type": "boolean", "description": "是否激活"}
 *   },
 *   "required": ["name", "age"]
 * }
 * }</pre>
 * 
 * <h3>链式调用（Fluent API）</h3>
 * <p>每个 add* 方法都返回 this，允许连续调用多个方法。
 * 这种设计模式叫做"构建者模式"（Builder Pattern）的变体。</p>
 */
@Getter
public class JsonObjectSchema {

    /**
     * Jackson ObjectMapper 实例
     * 
     * <p>用于创建 JSON 节点。声明为 static final 以复用实例。</p>
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Schema 的根节点
     * 
     * <p>ObjectNode 是 Jackson 中表示 JSON 对象的类。
     * 它是可变的，可以动态添加属性。</p>
     */
    private final ObjectNode schema;

    /**
     * 构造函数
     * 
     * <p>初始化一个基本的 object 类型 Schema：</p>
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {}
     * }
     * }</pre>
     */
    public JsonObjectSchema() {
        // 创建根节点
        this.schema = MAPPER.createObjectNode();
        // 设置类型为 object
        this.schema.put("type", "object");
        // 创建空的 properties 对象
        this.schema.putObject("properties");
    }

    /**
     * 添加字符串类型属性
     * 
     * <p>在 properties 中添加一个 string 类型的字段定义。</p>
     * 
     * @param name 属性名称
     * @param description 属性描述
     * @return this，支持链式调用
     */
    public JsonObjectSchema addStringProperty(String name, String description) {
        // 获取 properties 节点
        ObjectNode properties = (ObjectNode) schema.get("properties");
        // 在 properties 下创建新的属性节点
        ObjectNode prop = properties.putObject(name);
        // 设置类型和描述
        prop.put("type", "string");
        prop.put("description", description);
        // 返回 this 支持链式调用
        return this;
    }

    /**
     * 添加整数类型属性
     * 
     * @param name 属性名称
     * @param description 属性描述
     * @return this，支持链式调用
     */
    public JsonObjectSchema addIntegerProperty(String name, String description) {
        ObjectNode properties = (ObjectNode) schema.get("properties");
        ObjectNode prop = properties.putObject(name);
        prop.put("type", "integer");
        prop.put("description", description);
        return this;
    }

    /**
     * 添加布尔类型属性
     * 
     * @param name 属性名称
     * @param description 属性描述
     * @return this，支持链式调用
     */
    public JsonObjectSchema addBooleanProperty(String name, String description) {
        ObjectNode properties = (ObjectNode) schema.get("properties");
        ObjectNode prop = properties.putObject(name);
        prop.put("type", "boolean");
        prop.put("description", description);
        return this;
    }

    /**
     * 设置必填字段
     * 
     * <p>在 Schema 中添加 required 数组，指定哪些字段是必填的。</p>
     * 
     * <h3>Stream API 说明</h3>
     * <p>这里使用了 Java 8 的 Stream API：</p>
     * <ol>
     *   <li>required.stream() - 将 List 转换为 Stream</li>
     *   <li>.map(MAPPER::valueToTree) - 将每个字符串转换为 JsonNode</li>
     *   <li>.map(JsonNode.class::cast) - 类型转换</li>
     *   <li>.toList() - 收集为 List</li>
     * </ol>
     * 
     * @param required 必填字段名称列表
     * @return this，支持链式调用
     */
    public JsonObjectSchema setRequired(List<String> required) {
        // putArray 创建一个 JSON 数组节点
        // addAll 将所有元素添加到数组中
        schema.putArray("required").addAll(
            required.stream()
                // 将字符串转换为 JsonNode（TextNode）
                .map(MAPPER::valueToTree)
                // 类型转换为 JsonNode
                .map(JsonNode.class::cast)
                // 收集为 List
                .toList()
        );
        return this;
    }

    /**
     * 转换为 JsonNode
     * 
     * <p>返回构建好的 Schema。通常在链式调用的最后调用此方法。</p>
     * 
     * @return 完整的 JSON Schema
     */
    public JsonNode toJsonNode() {
        return schema;
    }
}
