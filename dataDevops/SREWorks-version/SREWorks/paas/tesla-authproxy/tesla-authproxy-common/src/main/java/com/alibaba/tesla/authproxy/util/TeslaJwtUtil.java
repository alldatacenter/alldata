package com.alibaba.tesla.authproxy.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.exceptions.TeslaJwtException;

/**
 * @author tandong.td@alibaba-inc.com
 */
@Slf4j
public class TeslaJwtUtil {

    public static final int JWT_TOKEN_TIMEOUT = 86400000;
    public static final int ALLOWED_CLOCK_SKEW_SECONDS = 300;

    /**
     * @param token
     * @param secretKey
     * @return
     */
    public static Claims verify(String token, String secretKey) throws TeslaJwtException {
        try {
            Claims claims = Jwts.parser().setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS).setSigningKey(
                secretKey.getBytes(Charset.forName("UTF-8")))
                .parseClaimsJws(token).getBody();
            return claims;
        } catch (Exception ex) {
            throw new TeslaJwtException("Verify tesla jwt token failed!", ex);
        }
    }

    public static String create(String empId, long ttlMillis, String secretKey) {
        /**
         * 添加构成JWT的参数
         */
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
            .claim(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, empId)
            .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8));

        /**
         * 添加Token过期时间
         */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }

    public static String create(String empId, String loginName, String bucId, long ttlMillis, String secretKey) {
        /**
         * 添加构成JWT的参数
         */
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
            .claim(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, empId)
            .claim(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, loginName)
            .claim(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, bucId)
            .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8));

        /**
         * 添加Token过期时间
         */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }

    public static String create(String empId, String loginName, String bucId, String email, long ttlMillis,
        String secretKey) {
        /**
         * 添加构成JWT的参数
         */
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
            .claim(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, empId)
            .claim(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, loginName)
            .claim(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, bucId)
            .claim(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY, email)
            .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8));

        /**
         * 添加Token过期时间
         */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }

    public static String create(String empId, String loginName, String bucId, String email, String userId,
        String nickName, long ttlMillis, String secretKey) {
        /**
         * 添加构成JWT的参数
         */
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
            .claim(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, empId)
            .claim(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, loginName)
            .claim(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, bucId)
            .claim(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY, email)
            .claim(AuthJwtConstants.JWT_USER_ID_CLAIM_KEY, userId)
            .claim(AuthJwtConstants.JWT_NICKNAME_CLAIM_KEY, nickName)
            .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8));

        /**
         * 添加Token过期时间
         */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }

    public static String create(String empId, String loginName, String bucId, String email, String userId,
        String nickName, String aliyunPk, long ttlMillis, String secretKey) {
        /**
         * 添加构成JWT的参数
         */
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
            .claim(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY, empId)
            .claim(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY, loginName)
            .claim(AuthJwtConstants.JWT_BUC_ID_CLAIM_KEY, bucId)
            .claim(AuthJwtConstants.JWT_EMAIL_CLAIM_KEY, email)
            .claim(AuthJwtConstants.JWT_USER_ID_CLAIM_KEY, userId)
            .claim(AuthJwtConstants.JWT_NICKNAME_CLAIM_KEY, nickName)
            .claim(AuthJwtConstants.JWT_ALIYUN_PK_CLAIM_KEY, aliyunPk)
            .signWith(signatureAlgorithm, secretKey.getBytes(StandardCharsets.UTF_8));

        /**
         * 添加Token过期时间
         */
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }
}
