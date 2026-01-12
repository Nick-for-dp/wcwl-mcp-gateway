package com.wcwl.mcpgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * MCP工具清单响应
 */
@Data
@AllArgsConstructor
public class McpManifestResponse {

    @JsonProperty("tools")
    private List<ToolDefinition> tools;

    /**
     * 工具定义
     */
    @Data
    @AllArgsConstructor
    public static class ToolDefinition {

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("inputSchema")
        private JsonNode inputSchema;
    }
}
