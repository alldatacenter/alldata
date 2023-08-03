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
package io.datavines.server.utils;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import io.datavines.core.constant.DataVinesConstants;
import io.datavines.core.utils.TokenManager;
import io.datavines.server.api.dto.vo.KaptchaResp;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VerificationUtil {

    private static final Logger logger = LoggerFactory.getLogger(VerificationUtil.class);
    private static final DefaultKaptcha defaultKaptcha;
    private static final TokenManager tokenManager;
    private static final Long timeOutMillis = 300000L;

    static {
        defaultKaptcha = SpringApplicationContext.getBean(DefaultKaptcha.class);

        tokenManager = SpringApplicationContext.getBean(TokenManager.class);
    }

    public static KaptchaResp createVerificationCodeAndImage() {
        String verificationCode = defaultKaptcha.createText();
        return KaptchaResp.builder()
                .imageByte64(buildImageByte64(verificationCode))
                .verificationCodeJwt(buildJwtVerification(verificationCode))
                .build();
    }

    public static void validVerificationCode(String verificationCode, String verificationCodeJwt) throws DataVinesServerException {
        Claims claims;
        try {
            claims = tokenManager.getClaims(verificationCodeJwt);
        } catch (ExpiredJwtException e) {
            throw new DataVinesServerException(Status.EXPIRED_VERIFICATION_CODE);
        }
        Date expiration = claims.getExpiration();
        if(null == expiration || expiration.getTime() < System.currentTimeMillis()){
            throw new DataVinesServerException(Status.EXPIRED_VERIFICATION_CODE);
        }
        String verificationCodeInJwt = null == claims.get(DataVinesConstants.TOKEN_VERIFICATION_CODE) ? null : claims.get(DataVinesConstants.TOKEN_VERIFICATION_CODE).toString();
        if(null == verificationCodeInJwt || !verificationCodeInJwt.equals(verificationCode)){
            throw new DataVinesServerException(Status.INVALID_VERIFICATION_CODE);
        }
    }

    private static String buildJwtVerification(String verificationCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(DataVinesConstants.TOKEN_VERIFICATION_CODE, verificationCode);
        claims.put(DataVinesConstants.TOKEN_CREATE_TIME, System.currentTimeMillis());
        return tokenManager.toTokenString(timeOutMillis, claims);
    }

    private static String buildImageByte64(String verificationCode) {
        BufferedImage image = defaultKaptcha.createImage(verificationCode);
        ByteArrayOutputStream outputStream = null;
        byte[] imageInByte = null;
        try {
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", outputStream);
            outputStream.flush();
            imageInByte = outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("image to byte exception cause of :", e);
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.error("close outputStream exception cause of :", e);
                }
            }
        }
        String imageByte64;
        BASE64Encoder encoder = new BASE64Encoder();
        if (null == imageInByte) {
            throw new DataVinesServerException(Status.CREATE_VERIFICATION_IMAGE_ERROR);
        }
        imageByte64 = encoder.encodeBuffer(imageInByte).replaceAll("\n", "").replaceAll("\r", "");
        return "data:image/jpg;base64,".concat(imageByte64);
    }

    public static boolean verifyIsNeedParam(Map<String ,String>  parameter, String[]  times) {
        for (String time : times) {
            if (!parameter.containsKey(time)) {
                return false;
            }
            try {
                int timeValue = Integer.parseInt(parameter.get(time));
                if (timeValue > 60 || timeValue < 0) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return  true;
    }
}


