package com.alibaba.sreworks.common.util;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

import com.alibaba.fastjson.JSONObject;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * @author jinghua.yjh
 */
public class JwtUtil {

    /**
     * 生成Jwt
     */
    public static String buildJwtRs256(JSONObject payload, String key)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JWT");
        builder.setIssuer("sreworks")
            .setSubject("token")
            .setExpiration(new Date(System.currentTimeMillis() + 86400 * 1000 + 120000))
            .setIssuedAt(new Date())
            .setNotBefore(new Date())
            .signWith(signatureAlgorithm, privateKey);

        builder.addClaims(payload);
        return builder.compact();
    }

    /**
     * 解密Jwt内容
     */
    public static Claims parseJwtRs256(String jwt, String key)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        return Jwts.parser()
            .setSigningKey(publicKey)
            .parseClaimsJws(jwt).getBody();

    }

}

