package com.wcwl.mcpgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * MCP工具执行成功响应
 */
@Data
@AllArgsConstructor
public class McpSuccessResponse {

    @JsonProperty("result")
    private Object result;
}
