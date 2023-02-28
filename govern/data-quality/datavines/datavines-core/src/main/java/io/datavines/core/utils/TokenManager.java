/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.core.utils;

import io.datavines.core.constant.DataVinesConstants;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.datavines.common.entity.TokenInfo;

@Component
public class TokenManager {

    @Value("${jwt.token.secret:asdqwe}")
    private String tokenSecret;

    @Value("${jwt.token.timeout:8640000}")
    private Long timeout;

    @Value("${jwt.token.algorithm:HS512}")
    private String algorithm;

    public String generateToken(String username, String password) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(DataVinesConstants.TOKEN_USER_NAME, StringUtils.isEmpty(username) ? DataVinesConstants.EMPTY : username);
        claims.put(DataVinesConstants.TOKEN_USER_PASSWORD, StringUtils.isEmpty(password) ? DataVinesConstants.EMPTY : password);
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    public String generateToken(TokenInfo tokenInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(DataVinesConstants.TOKEN_USER_NAME, StringUtils.isEmpty(tokenInfo.getUsername()) ? DataVinesConstants.EMPTY : tokenInfo.getUsername());
        claims.put(DataVinesConstants.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenInfo.getPassword()) ? DataVinesConstants.EMPTY : tokenInfo.getPassword());
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    public String refreshToken(String token) {
        Claims claims = getClaims(token);
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return generate(claims);
    }

    public String generateToken(TokenInfo tokenInfo, Long timeOutMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(DataVinesConstants.TOKEN_USER_NAME, StringUtils.isEmpty(tokenInfo.getUsername()) ? DataVinesConstants.EMPTY : tokenInfo.getUsername());
        claims.put(DataVinesConstants.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenInfo.getPassword()) ? DataVinesConstants.EMPTY : tokenInfo.getPassword());
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());

        return toTokenString(timeOutMillis, claims);
    }

    public String generateContinuousToken(TokenInfo tokenInfo) {
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put(DataVinesConstants.TOKEN_USER_NAME, StringUtils.isEmpty(tokenInfo.getUsername()) ? DataVinesConstants.EMPTY : tokenInfo.getUsername());
        claims.put(DataVinesConstants.TOKEN_USER_PASSWORD, StringUtils.isEmpty(tokenInfo.getPassword()) ? DataVinesConstants.EMPTY : tokenInfo.getPassword());
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());
        SignatureAlgorithm.valueOf(algorithm);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(claims.get(DataVinesConstants.TOKEN_USER_NAME).toString())
                .signWith(SignatureAlgorithm.valueOf(algorithm), tokenSecret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    private String generate(Map<String, Object> claims) {
        return toTokenString(timeout, claims);
    }

    public String toTokenString(Long timeOutMillis, Map<String, Object> claims) {
        long expiration = Long.parseLong(claims.get(DataVinesConstants.TOKEN_CREATE_TIME) + DataVinesConstants.EMPTY) + timeOutMillis;

        SignatureAlgorithm.valueOf(algorithm);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(null == claims.get(DataVinesConstants.TOKEN_USER_NAME) ? null : claims.get(DataVinesConstants.TOKEN_USER_NAME).toString())
                .setExpiration(new Date(expiration))
                .signWith(SignatureAlgorithm.valueOf(algorithm), tokenSecret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public String getUsername(String token) {
        String username = null;
        try {
            final Claims claims = getClaims(token);
            username = claims.get(DataVinesConstants.TOKEN_USER_NAME).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return username;
    }

    public String getPassword(String token) {
        String password = null;
        try {
            final Claims claims = getClaims(token);
            password = claims.get(DataVinesConstants.TOKEN_USER_PASSWORD).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return password;
    }

    public Claims getClaims(String token) {
       return Jwts.parser()
                    .setSigningKey(tokenSecret.getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token.startsWith(DataVinesConstants.TOKEN_PREFIX) ?
                            token.substring(token.indexOf(DataVinesConstants.TOKEN_PREFIX) + DataVinesConstants.TOKEN_PREFIX.length()).trim() :
                            token.trim())
                    .getBody();
    }

    public boolean validateToken(String token, String username, String password) {
        String tokenUsername = getUsername(token);
        String tokenPassword = getPassword(token);
        return (username.equals(tokenUsername) && password.equals(tokenPassword) && !(isExpired(token)));
    }

    private Date getCreatedDate(String token) {
        Date created = null;
        try {
            final Claims claims = getClaims(token);
            created = new Date((Long) claims.get(DataVinesConstants.TOKEN_CREATE_TIME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return created;
    }

    private Date getExpirationDate(String token) {
        Date expiration = null;
        try {
            final Claims claims = getClaims(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expiration;
    }

    private Boolean isExpired(String token) {
        final Date expiration = getExpirationDate(token);
        return null != expiration && expiration.before(new Date(System.currentTimeMillis()));
    }

}
