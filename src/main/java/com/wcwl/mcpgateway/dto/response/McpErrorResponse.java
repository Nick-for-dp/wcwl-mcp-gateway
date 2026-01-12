package com.wcwl.mcpgateway.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class McpErrorResponse {
    
    @JsonProperty("error")
    private final String error;        // 错误名称，如 "invalid_param"
    
    @JsonProperty("message")
    private final String message;      // 错误描述
    
    @JsonProperty("code")
    private final Integer code;        // HTTP 状态码（可选）
}
