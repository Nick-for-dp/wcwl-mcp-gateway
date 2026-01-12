package com.wcwl.mcpgateway.controller.admin;

import com.wcwl.mcpgateway.common.constant.ErrorCodes;
import com.wcwl.mcpgateway.common.exception.McpToolException;
import com.wcwl.mcpgateway.dto.request.ToolRegisterRequest;
import com.wcwl.mcpgateway.model.mcp.McpTool;
import com.wcwl.mcpgateway.service.tool.ToolRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工具管理控制器（仅 ADMIN 角色可访问）
 * 
 * <p>这个控制器提供工具的动态管理功能，允许管理员在运行时注册新工具。</p>
 * 
 * <h3>访问权限</h3>
 * <p>根据 SecurityConfig 的配置，/admin/** 路径需要 ADMIN 角色才能访问。</p>
 * 
 * <h3>动态注册工具的流程</h3>
 * <ol>
 *   <li>接收工具类的全限定类名</li>
 *   <li>使用反射加载类</li>
 *   <li>验证类是否实现了 McpTool 接口</li>
 *   <li>创建实例并进行 Spring 依赖注入</li>
 *   <li>注册到 ToolRegistry</li>
 * </ol>
 * 
 * <h3>API 使用示例</h3>
 * <pre>
 * POST /admin/tools/register
 * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9... (需要 ADMIN 角色)
 * Content-Type: application/json
 * 
 * {
 *   "className": "com.wcwl.mcpgateway.tools.builtin.SampleWarehouseTool"
 * }
 * </pre>
 * 
 * @see ToolRegistryService 工具注册中心接口
 */
@RestController
@RequestMapping("/admin/tools")
public class ToolAdminController {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ToolAdminController.class);

    /**
     * 工具注册中心服务
     */
    private final ToolRegistryService toolRegistry;
    
    /**
     * Spring Bean 工厂
     * 
     * <p>用于对动态创建的对象进行依赖注入。
     * 当我们通过反射创建对象时，Spring 不会自动注入依赖，
     * 需要手动调用 autowireBean() 方法。</p>
     */
    private final AutowireCapableBeanFactory beanFactory;

    /**
     * 构造函数注入
     * 
     * @param toolRegistry 工具注册中心服务
     * @param beanFactory Spring Bean 工厂
     */
    public ToolAdminController(ToolRegistryService toolRegistry, 
                               AutowireCapableBeanFactory beanFactory) {
        this.toolRegistry = toolRegistry;
        this.beanFactory = beanFactory;
    }

    /**
     * 动态注册工具
     * 
     * <h3>实现原理</h3>
     * <p>使用 Java 反射机制动态加载和实例化类：</p>
     * <ul>
     *   <li>Class.forName() - 根据类名加载类</li>
     *   <li>clazz.getDeclaredConstructor().newInstance() - 创建实例</li>
     *   <li>beanFactory.autowireBean() - 注入依赖</li>
     * </ul>
     * 
     * <h3>安全注意事项</h3>
     * <p>动态加载类存在安全风险，生产环境应该：</p>
     * <ul>
     *   <li>限制可加载的类（白名单机制）</li>
     *   <li>验证类来源</li>
     *   <li>记录审计日志</li>
     * </ul>
     * 
     * @param request 包含工具类名的请求
     * @return 注册结果
     * @throws McpToolException 当注册失败时抛出
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerTool(
            @RequestBody ToolRegisterRequest request) {

        String className = request.getClassName();
        
        // 参数校验
        if (className == null || className.isBlank()) {
            throw new McpToolException(ErrorCodes.INVALID_PARAM, 
                    "className is required", 400);
        }

        try {
            // ========== 1. 加载类 ==========
            // Class.forName() 根据全限定类名加载类
            // 如果类不存在，会抛出 ClassNotFoundException
            Class<?> clazz = Class.forName(className);

            // ========== 2. 验证是否实现 McpTool 接口 ==========
            // isAssignableFrom() 检查 clazz 是否是 McpTool 的子类或实现类
            if (!McpTool.class.isAssignableFrom(clazz)) {
                throw new McpToolException(ErrorCodes.INVALID_CLASS, 
                        "Class must implement McpTool interface", 400);
            }

            // ========== 3. 实例化并自动装配 ==========
            // 使用无参构造函数创建实例
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // 注入 @Autowired 依赖
            beanFactory.autowireBean(instance);
            // 执行初始化回调（如 @PostConstruct）
            beanFactory.initializeBean(instance, className);

            // ========== 4. 注册到 ToolRegistry ==========
            McpTool tool = (McpTool) instance;
            toolRegistry.register(tool);

            log.info("Tool registered dynamically: {}", tool.getName());

            // 返回成功响应
            return ResponseEntity.ok(Map.of(
                    "message", "Tool registered successfully",
                    "toolName", tool.getName()
            ));

        } catch (ClassNotFoundException e) {
            // 类不存在
            throw new McpToolException(ErrorCodes.CLASS_NOT_FOUND, 
                    "Class not found: " + className, 404);
        } catch (McpToolException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他异常（如反射异常）
            log.error("Failed to register tool: {}", className, e);
            throw new McpToolException(ErrorCodes.REGISTRATION_FAILED, 
                    "Failed to register tool: " + e.getMessage(), 500);
        }
    }
}
