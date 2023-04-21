package org.dromara.cloudeon.test;

import cn.hutool.crypto.digest.MD5;

import java.security.NoSuchAlgorithmException;

public class PasswordUtilTest {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        // 495c2bbdc8c80d8a16b60311e34667ae
        String passwd = MD5.create().digestHex("admin"+"ioAs7orSYY2fd0PeOgKf907A1l9MwycE");

        System.out.println(passwd);
    }
}
