package com.wcwl.mcpgateway.common.constant;

/**
 * 错误码常量类
 * 
 * <p>集中管理所有错误码，避免硬编码在代码中。
 * 使用常量类的好处：</p>
 * <ul>
 *   <li>统一管理 - 所有错误码在一个地方定义</li>
 *   <li>避免拼写错误 - IDE 会提示常量名</li>
 *   <li>便于修改 - 修改一处，全局生效</li>
 *   <li>便于查找 - 可以快速找到所有使用该错误码的地方</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * throw new McpToolException(ErrorCodes.TOOL_NOT_FOUND, "Tool not found: xxx", 404);
 * }</pre>
 */
public final class ErrorCodes {

    /**
     * 私有构造函数，防止实例化
     * 
     * <p>常量类不应该被实例化，所有成员都是静态的。</p>
     */
    private ErrorCodes() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ==================== 认证相关错误码 ====================
    
    /**
     * 未认证 - 用户未提供有效的认证信息
     */
    public static final String UNAUTHORIZED = "unauthorized";
    
    /**
     * 权限不足 - 用户已认证但没有访问权限
     */
    public static final String FORBIDDEN = "forbidden";

    // ==================== 工具相关错误码 ====================
    
    /**
     * 工具不存在
     */
    public static final String TOOL_NOT_FOUND = "tool_not_found";
    
    /**
     * 工具执行错误
     */
    public static final String TOOL_EXECUTION_ERROR = "tool_execution_error";
    
    /**
     * 工具注册失败
     */
    public static final String REGISTRATION_FAILED = "registration_failed";

    // ==================== 参数相关错误码 ====================
    
    /**
     * 参数无效
     */
    public static final String INVALID_PARAM = "invalid_param";
    
    /**
     * 参数校验失败
     */
    public static final String VALIDATION_ERROR = "validation_error";

    // ==================== 类加载相关错误码 ====================
    
    /**
     * 类不存在
     */
    public static final String CLASS_NOT_FOUND = "class_not_found";
    
    /**
     * 类无效（如未实现指定接口）
     */
    public static final String INVALID_CLASS = "invalid_class";

    // ==================== 通用错误码 ====================
    
    /**
     * 内部服务器错误
     */
    public static final String INTERNAL_ERROR = "internal_error";
}
