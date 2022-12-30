package com.alibaba.tesla.authproxy.util;

import com.alibaba.tesla.authproxy.constants.AuthJwtConstants;
import com.alibaba.tesla.authproxy.exceptions.TeslaJwtException;

import io.jsonwebtoken.Claims;
import org.junit.Test;

import static org.junit.Assert.*;

public class TeslaJwtUtilTest {

    @Test
    public void verify() throws TeslaJwtException {
        String secretKey = "sdfafdsaf23432f2";
        String token = TeslaJwtUtil.create("zhangsan", 86400000, secretKey);
        System.out.println("token:" + token);
        Claims claims = TeslaJwtUtil.verify(token, secretKey);
        System.out.println("" + claims.get(""));
        System.out.println("CLAIM_KEY:" + claims.get(AuthJwtConstants.JWT_EMP_ID_CLAIM_KEY));
        System.out.println("LOGIN_NAME_CLAIM_KEY:" + claims.get(AuthJwtConstants.JWT_LOGIN_NAME_CLAIM_KEY));
        System.out.println(claims);
        assertNotNull(claims);
    }

}