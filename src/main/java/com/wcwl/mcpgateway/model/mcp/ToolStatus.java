package com.wcwl.mcpgateway.model.mcp;

/**
 * 工具状态枚举
 */
public enum ToolStatus {

    /**
     * 草稿 - 新注册的工具，尚未发布
     */
    DRAFT("草稿"),

    /**
     * 待审核 - 已提交审核，等待管理员审批
     */
    PENDING_REVIEW("待审核"),

    /**
     * 已发布/上架 - 审核通过，可被用户使用
     */
    PUBLISHED("已发布"),

    /**
     * 已下架 - 暂停使用，但保留配置
     */
    OFFLINE("已下架"),

    /**
     * 已拒绝 - 审核未通过
     */
    REJECTED("已拒绝");

    private final String displayName;

    ToolStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
