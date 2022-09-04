package com.alibaba.tesla.authproxy.util;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;


/**
 * @author tandong.td@alibaba-inc.com
 */
@Slf4j
public class JwtUtil {

    private static final String DEFAULT_KEY = "tesla#JwtUtil_key2019-03-21";

    /**
     *
     * @param token
     * @param secretKey
     * @return
     */
    public static Claims verify(String token, String secretKey){
        try {
            if(StringUtil.isEmpty(secretKey)){
                secretKey = DEFAULT_KEY;
            }
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
            /**
             * 生成签名密钥
             */
            byte[] keyBytes = DatatypeConverter.parseBase64Binary(secretKey);
            Key sign = new SecretKeySpec(keyBytes, signatureAlgorithm.getJcaName());

            Claims claims = Jwts.parser().setSigningKey(sign)
                    .parseClaimsJws(token).getBody();
            return claims;
        } catch(Exception e) {
            log.error("JWT verify failed", e);
            return null;
        }
    }

    public static String create(String loginName, long ttlMillis, String secretKey) {

        if(StringUtil.isEmpty(secretKey)){
           secretKey = DEFAULT_KEY;
        }
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        /**
         * 生成签名密钥
         */
        byte[] keyBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key sign = new SecretKeySpec(keyBytes, signatureAlgorithm.getJcaName());

        /**
         * 添加构成JWT的参数
         */
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT")
                .claim("tesla_user_id", loginName)
                .signWith(signatureAlgorithm, sign);
        /**
         * 添加Token过期时间
         */
        if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp).setNotBefore(now);
        }
        return builder.compact();
    }
}
