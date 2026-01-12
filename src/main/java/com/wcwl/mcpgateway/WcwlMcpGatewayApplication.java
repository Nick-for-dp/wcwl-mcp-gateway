package com.wcwl.mcpgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP Gateway 应用程序启动类
 * 
 * <p>这是整个应用的入口点。Spring Boot 会从这个类开始启动应用。</p>
 * 
 * <h3>什么是 @SpringBootApplication？</h3>
 * <p>这是一个组合注解，相当于同时使用了：</p>
 * <ul>
 *   <li>@Configuration - 标记这是一个配置类</li>
 *   <li>@EnableAutoConfiguration - 启用 Spring Boot 的自动配置机制</li>
 *   <li>@ComponentScan - 自动扫描当前包及子包下的所有组件（@Component、@Service、@Controller 等）</li>
 * </ul>
 * 
 * <h3>MCP Gateway 是什么？</h3>
 * <p>MCP（Model Context Protocol）网关是一个工具管理和执行平台，主要功能包括：</p>
 * <ul>
 *   <li>工具注册 - 动态注册新的工具</li>
 *   <li>工具清单 - 提供所有可用工具的列表</li>
 *   <li>工具执行 - 根据名称调用指定工具</li>
 *   <li>权限控制 - 基于 JWT 的认证和基于角色的授权</li>
 * </ul>
 * 
 * @author WCWL Team
 * @version 1.0.0
 */
@SpringBootApplication
public class WcwlMcpGatewayApplication {

    /**
     * 应用程序主入口方法
     * 
     * <p>当你运行这个应用时，Java 会调用这个 main 方法。
     * SpringApplication.run() 会：</p>
     * <ol>
     *   <li>创建 Spring 应用上下文（ApplicationContext）</li>
     *   <li>扫描并注册所有的 Bean（组件）</li>
     *   <li>启动内嵌的 Tomcat 服务器</li>
     *   <li>开始监听 HTTP 请求（默认端口 8080）</li>
     * </ol>
     * 
     * @param args 命令行参数，可以用来覆盖配置，如：--server.port=9090
     */
    public static void main(String[] args) {
        SpringApplication.run(WcwlMcpGatewayApplication.class, args);
    }
}
