package com.wcwl.mcpgateway.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.dto.request.ToolRegisterRequest;
import com.wcwl.mcpgateway.dto.request.ToolRegisterRequest.ParamDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 动态代理工具
 * 
 * <p>根据配置动态创建的工具，执行时会调用配置的第三方服务端点。
 * 这是实现"无代码注册工具"的核心类。</p>
 * 
 * <h3>工作原理</h3>
 * <pre>
 * 1. 用户通过 API 传入工具配置
 * 2. 系统创建 DynamicProxyTool 实例
 * 3. 调用工具时，DynamicProxyTool 将请求转发到配置的 endpoint
 * 4. 将第三方服务的响应返回给调用者
 * </pre>
 * 
 * <h3>支持的功能</h3>
 * <ul>
 *   <li>自动将简化参数定义转换为 JSON Schema</li>
 *   <li>支持 GET/POST/PUT/DELETE 请求</li>
 *   <li>支持自定义请求头（如认证信息）</li>
 *   <li>支持超时配置</li>
 *   <li>支持角色权限控制</li>
 * </ul>
 */
public class DynamicProxyTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(DynamicProxyTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 工具配置信息
     */
    private final ToolRegisterRequest config;

    /**
     * 缓存的 JSON Schema（避免重复生成）
     */
    private final JsonNode inputSchema;

    /**
     * HTTP 客户端
     */
    private final RestTemplate restTemplate;

    /**
     * 构造函数
     * 
     * @param config 工具配置
     * @param restTemplate HTTP 客户端（由 Spring 管理，支持连接池）
     */
    public DynamicProxyTool(ToolRegisterRequest config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.inputSchema = buildInputSchema(config.getParams());
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getDescription() {
        return config.getDescription();
    }

    @Override
    public JsonNode getInputSchema() {
        return inputSchema;
    }

    @Override
    public Set<String> getRequiredRoles() {
        if (config.getRequiredRoles() == null || config.getRequiredRoles().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(config.getRequiredRoles());
    }

    /**
     * 执行工具 - 调用第三方服务
     */
    @Override
    protected JsonNode doExecute(Map<String, Object> args, Authentication auth) {
        String endpoint = config.getEndpoint();
        String method = config.getMethod().toUpperCase();

        log.info("Executing dynamic tool: name={}, endpoint={}, method={}", 
                config.getName(), endpoint, method);

        try {
            // 构建请求头
            HttpHeaders headers = buildHeaders();

            // 根据请求方法执行调用
            ResponseEntity<JsonNode> response;
            
            switch (method) {
                case "GET" -> {
                    // GET 请求：参数拼接到 URL
                    String urlWithParams = buildUrlWithParams(endpoint, args);
                    HttpEntity<Void> getEntity = new HttpEntity<>(headers);
                    response = restTemplate.exchange(urlWithParams, HttpMethod.GET, 
                            getEntity, JsonNode.class);
                }
                case "POST" -> {
                    HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(args, headers);
                    response = restTemplate.exchange(endpoint, HttpMethod.POST, 
                            postEntity, JsonNode.class);
                }
                case "PUT" -> {
                    HttpEntity<Map<String, Object>> putEntity = new HttpEntity<>(args, headers);
                    response = restTemplate.exchange(endpoint, HttpMethod.PUT, 
                            putEntity, JsonNode.class);
                }
                case "DELETE" -> {
                    String deleteUrl = buildUrlWithParams(endpoint, args);
                    HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
                    response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, 
                            deleteEntity, JsonNode.class);
                }
                default -> throw new McpToolException(ErrorCodes.INVALID_PARAM, 
                        "Unsupported HTTP method: " + method, 400);
            }

            // 检查响应
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                return body != null ? body : MAPPER.createObjectNode();
            } else {
                throw new McpToolException(ErrorCodes.TOOL_EXECUTION_ERROR,
                        "Remote service returned: " + response.getStatusCode(), 
                        response.getStatusCode().value());
            }

        } catch (RestClientException e) {
            log.error("Failed to call remote service: {}", e.getMessage(), e);
            throw new McpToolException(ErrorCodes.TOOL_EXECUTION_ERROR,
                    "Failed to call remote service: " + e.getMessage(), 502);
        }
    }

    /**
     * 将简化的参数定义转换为 JSON Schema
     */
    private JsonNode buildInputSchema(List<ParamDefinition> params) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        if (params != null) {
            for (ParamDefinition param : params) {
                // 添加属性定义
                ObjectNode prop = properties.putObject(param.getName());
                prop.put("type", param.getType());
                if (param.getDescription() != null) {
                    prop.put("description", param.getDescription());
                }
                if (param.getDefaultValue() != null) {
                    prop.set("default", MAPPER.valueToTree(param.getDefaultValue()));
                }

                // 添加到必填列表
                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }
        }

        return schema;
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // 添加自定义请求头
        if (config.getHeaders() != null) {
            config.getHeaders().forEach(headers::set);
        }

        return headers;
    }

    /**
     * 构建带参数的 URL（用于 GET/DELETE 请求）
     */
    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return sb.toString();
    }
}
