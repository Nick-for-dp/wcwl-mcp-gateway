package com.wcwl.mcpgateway.config;

import com.wcwl.mcpgateway.config.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 * 
 * <p>这个类配置了整个应用的安全策略，包括：</p>
 * <ul>
 *   <li>哪些 URL 需要认证</li>
 *   <li>哪些 URL 需要特定角色</li>
 *   <li>如何处理认证（JWT）</li>
 *   <li>会话管理策略</li>
 * </ul>
 * 
 * <h3>什么是 @Configuration？</h3>
 * <p>标记这是一个配置类，Spring 会在启动时处理这个类中的 @Bean 方法，
 * 将返回的对象注册为 Spring Bean。</p>
 * 
 * <h3>什么是 @EnableWebSecurity？</h3>
 * <p>启用 Spring Security 的 Web 安全功能。没有这个注解，
 * 安全配置不会生效。</p>
 * 
 * <h3>安全架构概述</h3>
 * <pre>
 * HTTP 请求
 *     ↓
 * JwtAuthFilter（验证 JWT Token）
 *     ↓
 * SecurityFilterChain（检查 URL 权限）
 *     ↓
 * Controller（处理业务逻辑）
 * </pre>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * JWT 认证过滤器
     * 
     * <p>通过构造函数注入。这个过滤器负责从请求头中提取 JWT Token
     * 并验证其有效性。</p>
     */
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * 配置安全过滤器链
     * 
     * <h3>什么是 @Bean？</h3>
     * <p>@Bean 标记的方法返回的对象会被 Spring 管理。
     * 这里返回的 SecurityFilterChain 定义了安全规则。</p>
     * 
     * <h3>配置说明</h3>
     * <ul>
     *   <li>CSRF 禁用 - 因为使用 JWT，不需要 CSRF 保护</li>
     *   <li>无状态会话 - 不使用 Session，每次请求都通过 JWT 认证</li>
     *   <li>路径权限 - 不同路径有不同的访问要求</li>
     *   <li>JWT 过滤器 - 在用户名密码认证之前执行</li>
     * </ul>
     * 
     * @param http HttpSecurity 配置对象
     * @return 配置好的安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ========== CSRF 配置 ==========
            // CSRF（跨站请求伪造）保护通常用于基于 Session 的认证
            // 使用 JWT 时可以禁用，因为 JWT 本身就能防止 CSRF 攻击
            .csrf(AbstractHttpConfigurer::disable)
            
            // ========== 会话管理 ==========
            // STATELESS 表示不创建 Session，每次请求都是独立的
            // 这是 RESTful API 的最佳实践
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // ========== URL 权限配置 ==========
            .authorizeHttpRequests(auth -> auth
                // /actuator/** - 健康检查、指标等端点，允许所有人访问
                .requestMatchers("/actuator/**").permitAll()
                
                // /admin/** - 管理接口，只有 ADMIN 角色可以访问
                // hasRole("ADMIN") 会自动匹配 "ROLE_ADMIN" 权限
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // /mcp/** - MCP 工具接口，需要认证（任何已登录用户）
                .requestMatchers("/mcp/**").authenticated()
                
                // 其他所有请求 - 允许访问（如静态资源）
                .anyRequest().permitAll()
            )
            
            // ========== 禁用表单登录 ==========
            // 我们使用 JWT 认证，不需要传统的登录表单
            .formLogin(AbstractHttpConfigurer::disable)
            
            // ========== 添加 JWT 过滤器 ==========
            // 在 UsernamePasswordAuthenticationFilter 之前执行
            // 这样 JWT 验证会先于其他认证方式
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
