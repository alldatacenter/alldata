package cn.datax.common.database.utils;

import java.security.MessageDigest;
import java.util.Arrays;

public class MD5Util {

    public static void main(String[] args) throws InterruptedException {
        Object[] arr = new Object[]{"dbName"};
        Object[] objects = Arrays.copyOf(arr, arr.length + 2);
        System.out.println(objects.length);
        int length = arr.length;
        objects[length] = 1;
        objects[length+1] = 2;
        System.out.println(Arrays.toString(objects));
//        String encrypt = MD5Util.encrypt("sql" + ":" + Arrays.toString(arr));
//        System.out.println(encrypt);
    }

    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * MD5加密
     */
    public static String encrypt(String value){
        return encrypt(value.getBytes());
    }

    /**
     * MD5加密
     */
    public static String encrypt(byte[] value){
        try {
            byte[] bytes = MessageDigest.getInstance("MD5").digest(value);
            char[] chars = new char[32];
            for (int i = 0; i < chars.length; i = i + 2) {
                byte b = bytes[i / 2];
                chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
                chars[i + 1] = HEX_CHARS[b & 0xf];
            }
            return new String(chars);
        } catch (Exception e) {
            throw new RuntimeException("md5 encrypt error", e);
        }
    }
}
