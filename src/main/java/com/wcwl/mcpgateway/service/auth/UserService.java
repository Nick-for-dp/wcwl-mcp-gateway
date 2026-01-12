package com.wcwl.mcpgateway.service.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 用户服务
 * 
 * <p>这个服务实现了 Spring Security 的 UserDetailsService 接口，
 * 负责根据用户名加载用户信息。</p>
 * 
 * <h3>什么是 UserDetailsService？</h3>
 * <p>UserDetailsService 是 Spring Security 的核心接口之一。
 * 当需要验证用户身份时，Spring Security 会调用 loadUserByUsername()
 * 方法来获取用户信息。</p>
 * 
 * <h3>当前实现说明</h3>
 * <p>这是一个<b>示例实现</b>，使用内存中的 Mock 数据。
 * 生产环境应该：</p>
 * <ul>
 *   <li>从数据库查询用户信息</li>
 *   <li>使用加密的密码（如 BCrypt）</li>
 *   <li>实现用户注册、密码修改等功能</li>
 * </ul>
 * 
 * <h3>预定义用户</h3>
 * <table border="1">
 *   <tr><th>用户名</th><th>密码</th><th>角色</th></tr>
 *   <tr><td>admin</td><td>admin123</td><td>ADMIN, USER</td></tr>
 *   <tr><td>user</td><td>user123</td><td>USER, WAREHOUSE_VIEWER</td></tr>
 * </table>
 * 
 * @see UserDetails Spring Security 用户详情接口
 */
@Service
public class UserService implements UserDetailsService {

    /**
     * Mock 用户数据
     * 
     * <p>使用 Map.of() 创建不可变 Map，存储预定义的用户。</p>
     * 
     * <h3>密码格式说明</h3>
     * <p>{noop} 前缀表示密码是明文存储的（No Operation）。
     * 这只用于开发测试，生产环境应使用：</p>
     * <ul>
     *   <li>{bcrypt} - BCrypt 加密（推荐）</li>
     *   <li>{pbkdf2} - PBKDF2 加密</li>
     *   <li>{scrypt} - SCrypt 加密</li>
     * </ul>
     * 
     * <h3>User.builder() 说明</h3>
     * <p>Spring Security 提供的 User 类使用构建者模式，
     * 可以方便地创建 UserDetails 实例。</p>
     */
    private static final Map<String, UserDetails> USERS = Map.of(
            // 管理员用户
            "admin", User.builder()
                    .username("admin")
                    .password("{noop}admin123")  // {noop} 表示明文密码
                    .authorities(List.of(
                            // ROLE_ADMIN 角色，可以访问 /admin/** 接口
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            // ROLE_USER 角色
                            new SimpleGrantedAuthority("ROLE_USER")
                    ))
                    .build(),
            // 普通用户
            "user", User.builder()
                    .username("user")
                    .password("{noop}user123")
                    .authorities(List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            // 仓库查看权限
                            new SimpleGrantedAuthority("ROLE_WAREHOUSE_VIEWER")
                    ))
                    .build()
    );

    /**
     * 根据用户名加载用户信息
     * 
     * <p>这个方法会在以下场景被调用：</p>
     * <ul>
     *   <li>JWT 认证时，验证 Token 中的用户是否存在</li>
     *   <li>登录时，验证用户名密码</li>
     * </ul>
     * 
     * @param username 用户名
     * @return 用户详情
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从 Map 中查找用户
        UserDetails user = USERS.get(username);
        
        // 用户不存在时抛出异常
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        
        return user;
    }
}
