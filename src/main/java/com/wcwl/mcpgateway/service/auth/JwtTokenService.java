package com.wcwl.mcpgateway.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT 令牌服务
 * 
 * <p>这个服务负责 JWT（JSON Web Token）的生成、解析和验证。
 * JWT 是一种无状态的认证机制，服务器不需要存储会话信息。</p>
 * 
 * <h3>什么是 JWT？</h3>
 * <p>JWT 是一个由三部分组成的字符串，用点号分隔：</p>
 * <pre>
 * eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcwNDk2MDAwMH0.xxx
 * |_____Header_____|._______Payload_______|.___Signature___|
 * </pre>
 * <ul>
 *   <li>Header - 包含算法类型（如 HS256）</li>
 *   <li>Payload - 包含声明（Claims），如用户名、过期时间</li>
 *   <li>Signature - 使用密钥对前两部分签名，防止篡改</li>
 * </ul>
 * 
 * <h3>JWT 认证流程</h3>
 * <pre>
 * 1. 用户登录，服务器验证用户名密码
 * 2. 验证成功，服务器生成 JWT 返回给客户端
 * 3. 客户端保存 JWT（通常在 localStorage）
 * 4. 后续请求，客户端在 Authorization 头携带 JWT
 * 5. 服务器验证 JWT 有效性，提取用户信息
 * </pre>
 * 
 * <h3>@Value 注解</h3>
 * <p>@Value 用于从配置文件（application.yml）中注入值。
 * 冒号后面是默认值，当配置文件中没有定义时使用。</p>
 * 
 * @see JwtAuthFilter JWT 认证过滤器
 */
@Service
public class JwtTokenService {

    /**
     * JWT 签名密钥
     * 
     * <p>从配置文件 jwt.secret 读取，默认值仅用于开发环境。
     * 生产环境必须使用强密钥（至少 32 字符）。</p>
     * 
     * <p>密钥的安全性至关重要：</p>
     * <ul>
     *   <li>泄露密钥 = 任何人都可以伪造 Token</li>
     *   <li>密钥太短 = 容易被暴力破解</li>
     * </ul>
     */
    @Value("${jwt.secret:default-secret-key-for-development-only-change-in-production}")
    private String secret;

    /**
     * Token 过期时间（毫秒）
     * 
     * <p>默认 86400000 毫秒 = 24 小时</p>
     * <p>过期时间的权衡：</p>
     * <ul>
     *   <li>太短 - 用户需要频繁重新登录，体验差</li>
     *   <li>太长 - Token 泄露后风险窗口大</li>
     * </ul>
     */
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * 从令牌中提取用户名
     * 
     * <p>用户名存储在 JWT 的 "sub"（subject）声明中。</p>
     * 
     * @param token JWT 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        // Claims::getSubject 是方法引用，等价于 claims -> claims.getSubject()
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取过期时间
     * 
     * @param token JWT 令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从令牌中提取指定声明
     * 
     * <p>这是一个泛型方法，可以提取任意类型的声明。
     * 使用 Function 接口实现灵活的声明提取。</p>
     * 
     * <h3>什么是 Function？</h3>
     * <p>Function<T, R> 是 Java 8 引入的函数式接口，
     * 表示一个接受 T 类型参数、返回 R 类型结果的函数。</p>
     * 
     * @param token JWT 令牌
     * @param claimsResolver 声明解析函数
     * @param <T> 返回值类型
     * @return 提取的声明值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 验证令牌有效性
     * 
     * <p>验证条件：</p>
     * <ol>
     *   <li>Token 中的用户名与传入的用户名匹配</li>
     *   <li>Token 未过期</li>
     * </ol>
     * 
     * @param token JWT 令牌
     * @param userDetails 用户详情
     * @return true 如果令牌有效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * 生成 JWT 令牌
     * 
     * <p>创建一个包含用户名、签发时间、过期时间的 Token，
     * 并使用密钥进行签名。</p>
     * 
     * @param userDetails 用户详情
     * @return 生成的 JWT 令牌
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                // 设置主题（subject）为用户名
                .setSubject(userDetails.getUsername())
                // 设置签发时间为当前时间
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                // 使用密钥签名
                .signWith(getSigningKey())
                // 生成紧凑格式的 Token 字符串
                .compact();
    }

    /**
     * 解析令牌，提取所有声明
     * 
     * <p>这个方法会验证 Token 的签名。如果签名无效或 Token 格式错误，
     * 会抛出异常。</p>
     * 
     * @param token JWT 令牌
     * @return 所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                // 设置签名验证密钥
                .setSigningKey(getSigningKey())
                .build()
                // 解析 Token
                .parseClaimsJws(token)
                // 获取 Payload 部分（Claims）
                .getBody();
    }

    /**
     * 检查令牌是否已过期
     * 
     * @param token JWT 令牌
     * @return true 如果已过期
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 获取签名密钥
     * 
     * <p>将字符串密钥转换为 HMAC-SHA 密钥对象。
     * Keys.hmacShaKeyFor() 会根据密钥长度自动选择合适的算法
     * （HS256、HS384 或 HS512）。</p>
     * 
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
