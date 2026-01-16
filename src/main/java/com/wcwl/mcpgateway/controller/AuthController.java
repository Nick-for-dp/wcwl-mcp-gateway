package com.wcwl.mcpgateway.controller;

import com.wcwl.mcpgateway.service.auth.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 
 * 提供用户登录接口，验证用户名密码后返回JWT Token
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserDetailsService userDetailsService, 
                          JwtTokenService jwtTokenService,
                          PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     * 
     * @param request 包含 username 和 password 的请求体
     * @return JWT Token 和用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "用户名和密码不能为空"
            ));
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // 验证密码
            if (!passwordEncoder.matches(password, userDetails.getPassword())) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "用户名或密码错误"
                ));
            }

            // 生成JWT Token
            String token = jwtTokenService.generateToken(userDetails);

            // 提取角色列表
            var roles = userDetails.getAuthorities().stream()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token,
                    "username", username,
                    "roles", roles
            ));

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "用户名或密码错误"
            ));
        }
    }
}
