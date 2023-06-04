package org.dromara.cloudeon.utils;

import cn.hutool.crypto.digest.MD5;

public class PasswordUtil {
    public static String encode(String password) {
        return MD5.create().digestHex(password + Constant.SALT);
    }

    /**
     * 校验登录密码和数据库的密码
     * @param loginPassword
     * @param dbPassword
     * @return
     */
    public static boolean validate(String loginPassword,String dbPassword) {
        String digestHex = encode(loginPassword);
        return digestHex.equals(dbPassword);
    }
}
