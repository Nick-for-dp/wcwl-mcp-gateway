package com.wcwl.mcpgateway.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工具注册请求 DTO
 * 
 * <p>用于接收动态注册工具的请求参数。采用简化设计，
 * 用户无需了解 JSON Schema，只需提供简单的参数列表。</p>
 * 
 * <h3>使用示例</h3>
 * <pre>
 * POST /admin/tools/register
 * {
 *   "name": "query_trade_data",
 *   "description": "查询贸易数据",
 *   "endpoint": "http://api.example.com/trade/query",
 *   "method": "POST",
 *   "params": [
 *     {"name": "startDate", "type": "string", "required": true, "description": "开始日期"},
 *     {"name": "endDate", "type": "string", "required": true, "description": "结束日期"},
 *     {"name": "tradeType", "type": "string", "required": false, "description": "贸易类型"}
 *   ],
 *   "headers": {
 *     "X-API-Key": "your-api-key"
 *   },
 *   "timeout": 30000
 * }
 * </pre>
 */
@Data
public class ToolRegisterRequest {

    /**
     * 工具唯一名称
     * 
     * <p>用于 API 调用：POST /mcp/tools/{name}</p>
     * <p>建议使用小写字母和下划线，如：query_trade_data</p>
     */
    private String name;

    /**
     * 工具描述
     * 
     * <p>说明工具的功能，会在工具清单中展示</p>
     */
    private String description;

    /**
     * 目标服务端点 URL
     * 
     * <p>工具执行时实际调用的第三方服务地址</p>
     */
    private String endpoint;

    /**
     * HTTP 请求方法
     * 
     * <p>支持：GET、POST、PUT、DELETE，默认 POST</p>
     */
    private String method = "POST";

    /**
     * 参数定义列表（简化格式）
     * 
     * <p>系统会自动将其转换为标准的 JSON Schema</p>
     */
    private List<ParamDefinition> params;

    /**
     * 请求头配置
     * 
     * <p>调用第三方服务时附加的请求头，如认证信息</p>
     */
    private Map<String, String> headers;

    /**
     * 请求超时时间（毫秒）
     * 
     * <p>默认 30000ms（30秒）</p>
     */
    private Integer timeout = 30000;

    /**
     * 所需角色列表
     * 
     * <p>为空表示任何已认证用户都可以调用</p>
     */
    private List<String> requiredRoles;

    /**
     * 参数定义（简化格式）
     */
    @Data
    public static class ParamDefinition {

        /**
         * 参数名称
         */
        private String name;

        /**
         * 参数类型：string、integer、number、boolean、array、object
         */
        private String type = "string";

        /**
         * 是否必填
         */
        private boolean required = false;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 默认值（可选）
         */
        private Object defaultValue;
    }
}
