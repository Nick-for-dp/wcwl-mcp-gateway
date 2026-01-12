package com.wcwl.mcpgateway.config.filter;

import com.wcwl.mcpgateway.service.auth.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 
 * <p>这个过滤器负责从 HTTP 请求中提取 JWT Token，验证其有效性，
 * 并将认证信息设置到 Spring Security 的上下文中。</p>
 * 
 * <h3>什么是过滤器（Filter）？</h3>
 * <p>过滤器是 Servlet 规范中的概念，它可以在请求到达 Controller 之前
 * 或响应返回客户端之前进行拦截处理。常见用途：</p>
 * <ul>
 *   <li>认证和授权</li>
 *   <li>日志记录</li>
 *   <li>请求/响应修改</li>
 *   <li>跨域处理（CORS）</li>
 * </ul>
 * 
 * <h3>为什么继承 OncePerRequestFilter？</h3>
 * <p>OncePerRequestFilter 保证每个请求只执行一次过滤逻辑，
 * 即使请求被转发（forward）也不会重复执行。</p>
 * 
 * <h3>为什么放在 config.filter 包下？</h3>
 * <p>过滤器是配置层的一部分，与 SecurityConfig 紧密相关。
 * 将其放在 config.filter 包下更符合其职责定位。</p>
 * 
 * <h3>JWT 认证流程</h3>
 * <pre>
 * 1. 客户端在请求头中携带 Token: Authorization: Bearer xxx
 * 2. 过滤器提取 Token
 * 3. 验证 Token 有效性（签名、过期时间）
 * 4. 从 Token 中提取用户名
 * 5. 加载用户详情
 * 6. 创建认证对象并设置到 SecurityContext
 * 7. 后续代码可以通过 SecurityContext 获取当前用户信息
 * </pre>
 * 
 * @see JwtTokenService JWT Token 服务
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * JWT Token 服务，用于解析和验证 Token
     */
    private final JwtTokenService jwtTokenService;
    
    /**
     * 用户详情服务，用于加载用户信息
     */
    private final UserDetailsService userDetailsService;

    /**
     * 构造函数注入依赖
     * 
     * @param jwtTokenService JWT Token 服务
     * @param userDetailsService 用户详情服务
     */
    public JwtAuthFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 过滤器核心逻辑
     * 
     * <p>这个方法会在每个 HTTP 请求时被调用。</p>
     * 
     * <h3>参数说明</h3>
     * <ul>
     *   <li>request - HTTP 请求对象，可以获取请求头、参数等</li>
     *   <li>response - HTTP 响应对象，可以设置响应头、状态码等</li>
     *   <li>filterChain - 过滤器链，调用 doFilter 继续执行后续过滤器</li>
     * </ul>
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // ========== 1. 获取 Authorization 请求头 ==========
        // JWT Token 通常放在 Authorization 头中，格式为: Bearer <token>
        String authHeader = request.getHeader("Authorization");

        // 如果没有 Authorization 头，或者不是 Bearer 格式，跳过认证
        // 继续执行后续过滤器（可能是匿名访问的接口）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ========== 2. 提取 Token ==========
        // "Bearer eyJhbGciOiJIUzI1NiJ9..." -> "eyJhbGciOiJIUzI1NiJ9..."
        String token = authHeader.substring(7);  // 跳过 "Bearer " 前缀（7个字符）
        
        // ========== 3. 从 Token 中提取用户名 ==========
        String username = jwtTokenService.extractUsername(token);

        // ========== 4. 验证并设置认证信息 ==========
        // 条件：用户名存在 且 当前没有认证信息（避免重复认证）
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            // 从数据库/内存加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 验证 Token 有效性（签名正确、未过期、用户名匹配）
            if (jwtTokenService.validateToken(token, userDetails)) {
                
                // 创建认证令牌
                // 参数：principal（用户主体）、credentials（凭证，这里为null因为已验证）、authorities（权限列表）
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                
                // 设置认证详情（包含 IP 地址、Session ID 等）
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 将认证信息设置到 SecurityContext
                // 后续代码可以通过 SecurityContextHolder.getContext().getAuthentication() 获取
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // ========== 5. 继续执行过滤器链 ==========
        // 无论认证成功与否，都要调用这个方法，让请求继续处理
        filterChain.doFilter(request, response);
    }
}
