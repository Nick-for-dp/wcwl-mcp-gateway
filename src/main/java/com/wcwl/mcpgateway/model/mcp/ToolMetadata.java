package com.wcwl.mcpgateway.model.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP工具元数据
 * 
 * 包含工具的上传人、上传时间、分类、状态等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolMetadata {

    /**
     * 上传人用户名
     */
    private String createdBy;

    /**
     * 上传时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新人
     */
    private String updatedBy;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 工具分类
     */
    private String category;

    /**
     * 工具状态
     */
    private ToolStatus status;

    /**
     * 工具来源类型
     */
    private ToolSourceType sourceType;

    /**
     * 创建内置工具的默认元数据
     */
    public static ToolMetadata builtinDefault() {
        LocalDateTime now = LocalDateTime.now();
        return ToolMetadata.builder()
                .createdBy("system")
                .createdAt(now)
                .updatedBy("system")
                .updatedAt(now)
                .category("builtin")
                .status(ToolStatus.PUBLISHED)
                .sourceType(ToolSourceType.BUILTIN)
                .build();
    }

    /**
     * 创建动态注册工具的元数据
     */
    public static ToolMetadata forDynamicTool(String username, String category) {
        LocalDateTime now = LocalDateTime.now();
        return ToolMetadata.builder()
                .createdBy(username)
                .createdAt(now)
                .updatedBy(username)
                .updatedAt(now)
                .category(category != null ? category : "custom")
                .status(ToolStatus.DRAFT)  // 新注册的工具默认为草稿状态
                .sourceType(ToolSourceType.DYNAMIC)
                .build();
    }
}
