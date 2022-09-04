package com.alibaba.tdata.aisp.server.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.alibaba.tdata.aisp.server.common.exception.PlatformInternalException;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: MessageDigestUtil
 * @Author: dyj
 * @DATE: 2021-05-17
 * @Description:
 **/
@Slf4j
public class MessageDigestUtil {
    public static final String KEY_SHA_256 = "SHA-256";
    public static final String KEY_SHA_1 = "SHA-1";
    public static final String KEY_MD5 = "MD5";

    public static String genSHA256(String var) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(KEY_SHA_256);
        } catch (NoSuchAlgorithmException e){
            throw new PlatformInternalException("action=genSHA256||can not get digest object from MessageDigest");
        }
        digest.update(var.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = digest.digest();
        return encodeHex(bytes);
    }

    public static String genSHA1(String var) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(KEY_SHA_1);
        } catch (NoSuchAlgorithmException e){
            throw new PlatformInternalException("action=genSHA1||can not get digest object from MessageDigest");
        }
        digest.update(var.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = digest.digest();
        return encodeHex(bytes);
    }

    public static String genMD5(String var) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(KEY_MD5);
        } catch (NoSuchAlgorithmException e){
            throw new PlatformInternalException("action=genMD5||can not get digest object from MessageDigest");
        }
        digest.update(var.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = digest.digest();
        return encodeHex(bytes);
    }

    private static String encodeHex(byte[] byteBuffer) {
        StringBuffer strHexString = new StringBuffer();
        for (int i = 0; i < byteBuffer.length; i++)
        {
            String hex = Integer.toHexString(0xff & byteBuffer[i]);
            if (hex.length() == 1)
            {
                strHexString.append('0');
            }
            strHexString.append(hex);
        }
        return strHexString.toString();
    }
}
