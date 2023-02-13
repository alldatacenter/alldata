
package cn.datax.service.data.metadata.security;

import cn.datax.common.redis.service.RedisService;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import cn.datax.service.data.metadata.security.SecurityProperties;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author AllDataDC
 */
@Slf4j
@Component
public class TokenProvider implements InitializingBean {


    private final SecurityProperties properties = new SecurityProperties();
    @Autowired
    private RedisService redisUtils;
    public static final String AUTHORITIES_KEY = "user";
    private JwtParser jwtParser;
    private JwtBuilder jwtBuilder;



    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getBase64Secret());
        Key key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder()
                .setSigningKey(properties.getBase64Secret())
                .build();
        jwtBuilder = Jwts.builder()
                .signWith( SignatureAlgorithm.HS512,properties.getBase64Secret());
    }

    /**
     * 创建Token 设置永不过期，
     * Token 的时间有效性转到Redis 维护
     *
     * @param authentication /
     * @return /
     */
    public String createToken(Authentication authentication) {
        return jwtBuilder
                // 加入ID确保生成的 Token 都不一致
                .setId(IdUtil.simpleUUID())
                .claim(AUTHORITIES_KEY, authentication.getName())
                .setSubject(authentication.getName())
                .compact();
    }

    /**
     * 依据Token 获取鉴权信息
     *
     * @param token /
     * @return /
     */
    Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        User principal = new User(claims.getSubject(), "******", new ArrayList<>());
        return new UsernamePasswordAuthenticationToken(principal, token, new ArrayList<>());
    }

    public Claims getClaims(String token) {
        return jwtParser
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * @param token 需要检查的token
     */
    public void checkRenewal(String token) {
        // 判断是否续期token,计算token的过期时间
        long time = redisUtils.getExpire(properties.getOnlineKey() + token) * 1000;
        Date expireDate = DateUtil.offset(new Date(), DateField.MILLISECOND, (int) time);
        // 判断当前时间与过期时间的时间差
        long differ = expireDate.getTime() - System.currentTimeMillis();
        // 如果在续期检查的范围内，则续期
        if (differ <= properties.getDetect()) {
            long renew = time + properties.getRenew();
            redisUtils.expire(properties.getOnlineKey() + token, renew);
        }
    }

    public String getToken(HttpServletRequest request) {
        final String requestHeader = request.getHeader(properties.getHeader());
        if (requestHeader != null && requestHeader.startsWith(properties.getTokenStartWith())) {
            return requestHeader.substring(7);
        }
        return null;
    }
}
