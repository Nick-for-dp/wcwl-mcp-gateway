package com.wcwl.mcpgateway.model.mcp;

/**
 * 工具来源类型
 */
public enum ToolSourceType {

    /**
     * 内置工具 - 通过@Component注解静态注册
     */
    BUILTIN("内置"),

    /**
     * 动态工具 - 通过API动态注册
     */
    DYNAMIC("动态注册");

    private final String displayName;

    ToolSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
