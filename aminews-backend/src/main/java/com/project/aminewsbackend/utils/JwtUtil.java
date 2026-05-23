package com.project.aminewsbackend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Component // 将 JwtUtil 注册为 Spring Bean
public class JwtUtil {
    // 密钥
    @Value("${jwt.secret}")
    private String secretKey; // 移除 static 关键字
    // 过期时间
    @Value("${jwt.expiration}")
    private long expiration; // 移除 static 关键字

    /**
     * 加密算法
     */
    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;
    /**
     * 秘钥实例
     */
    // KEY 的初始化方式需要改变，因为 secretKey 不再是静态的
    private SecretKey KEY; // 声明为非静态，并在 init() 方法中初始化

    /**
     * jwt签发者
     */
    private final static String JWT_ISS = "Tiam";
    /**
     * jwt主题
     */
    private final static String SUBJECT = "Peripherals";

    // 在 Spring 注入完成后初始化 KEY
    // @PostConstruct 注解表示在 Spring Bean 初始化完成后执行此方法
    // 或者使用构造函数注入的方式
    // 这里为了简单，我们使用 @PostConstruct
    @jakarta.annotation.PostConstruct // 如果是 Spring Boot 3+，需要使用 jakarta.annotation.PostConstruct
    // 如果是 Spring Boot 2.x，使用 javax.annotation.PostConstruct
    public void init() {
        this.KEY = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /*
    这些是一组预定义的声明，它们 不是强制性的，而是推荐的 ，以 提供一组有用的、可互操作的声明 。
    iss: jwt签发者
    sub: jwt所面向的用户
    aud: 接收jwt的一方
    exp: jwt的过期时间，这个过期时间必须要大于签发时间
    nbf: 定义在什么时间之前，该jwt都是不可用的.
    iat: jwt的签发时间
    jti: jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
     */
    public String genAccessToken(int id) { // 移除 static 关键字
        // 令牌id
        String uuid = UUID.randomUUID().toString();
        // 注意：这里需要使用 this.expiration
        Date expireDate = Date.from(Instant.now().plusSeconds(this.expiration / 1000)); // 转换毫秒到秒

        return Jwts.builder()
                // 设置头部信息header
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                // 设置自定义负载信息payload
                .claim("id", id)
                // 令牌ID
                .id(uuid)
                // 过期日期
                .expiration(expireDate)
                // 签发时间
                .issuedAt(new Date())
                // 主题
                .subject(SUBJECT)
                // 签发者
                .issuer(JWT_ISS)
                // 签名
                .signWith(this.KEY, ALGORITHM) // 使用 this.KEY
                .compact();
    }

    /**
     * 解析token
     * @param token token
     * @return Jws<Claims>
     */
    public Jws<Claims> parseClaim(String token) { // 移除 static 关键字
        return Jwts.parser()
                .verifyWith(this.KEY) // 使用 this.KEY
                .build()
                .parseSignedClaims(token);
    }

    public JwsHeader parseHeader(String token) { // 移除 static 关键字
        return parseClaim(token).getHeader();
    }

    public Claims parsePayload(String token) { // 移除 static 关键字
        return parseClaim(token).getPayload();
    }
}