package com.wcwl.mcpgateway.dto.request;

import lombok.Data;

/**
 * 工具注册请求 DTO
 * 
 * <p>用于接收动态注册工具的请求参数。</p>
 * 
 * <h3>什么是 DTO？</h3>
 * <p>DTO（Data Transfer Object）是数据传输对象，用于在不同层之间传递数据。
 * 它通常只包含数据字段和 getter/setter 方法，不包含业务逻辑。</p>
 * 
 * <h3>@Data 注解</h3>
 * <p>Lombok 注解，自动生成：</p>
 * <ul>
 *   <li>所有字段的 getter 和 setter</li>
 *   <li>toString() 方法</li>
 *   <li>equals() 和 hashCode() 方法</li>
 *   <li>无参构造函数</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>
 * POST /admin/tools/register
 * Content-Type: application/json
 * 
 * {
 *   "className": "com.wcwl.mcpgateway.tools.builtin.SampleWarehouseTool"
 * }
 * </pre>
 */
@Data
public class ToolRegisterRequest {

    /**
     * 工具类的全限定类名
     * 
     * <p>例如：com.wcwl.mcpgateway.tools.builtin.SampleWarehouseTool</p>
     * 
     * <p>要求：</p>
     * <ul>
     *   <li>类必须存在于 classpath 中</li>
     *   <li>类必须实现 McpTool 接口</li>
     *   <li>类必须有无参构造函数</li>
     * </ul>
     */
    private String className;
}
