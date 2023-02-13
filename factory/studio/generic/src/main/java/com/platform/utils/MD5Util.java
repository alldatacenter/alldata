package com.platform.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

public class MD5Util {

    /** 向量(同时拥有向量和密匙才能解密)，此向量必须是8byte，多少都报错 */
    private final byte[] DESIV = new byte[] { 0x22, 0x54, 0x36, 110, 0x40, (byte) 0xac, (byte) 0xad, (byte) 0xdf };
    /** 自定义密钥,个数不能太短，太短报错，过长，它默认只取前N位（N的具体值，大家另行查找资料） */
    private final String deSkey = "cloud";
    /** 加密算法的参数接口 */
    private AlgorithmParameterSpec iv = null;
    private Key key = null;
    private String charset = "UTF-8";

    private static volatile MD5Util instance;

    /**
     * 构造函数
     * @throws Exception
     */
    private MD5Util() throws Exception {
        // 设置密钥参数
        DESKeySpec keySpec = new DESKeySpec(deSkey.getBytes(this.charset));
        // 设置向量
        iv = new IvParameterSpec(DESIV);
        // 获得密钥工厂
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        // 得到密钥对象
        key = keyFactory.generateSecret(keySpec);
    }

    public static MD5Util getInstance() throws Exception {
        if(instance == null) {
            synchronized (MD5Util.class) {
                if(instance == null) {
                    instance = new MD5Util();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        try {
            String value = "1246656415670484994";
            MD5Util mt = new MD5Util();
            System.out.println("加密前的字符：" + value);
            System.out.println("加密后的字符：" + mt.encode(value));
            System.out.println("解密后的字符：" + mt.decode(mt.encode(value)));
            System.out.println("字符串的MD5值："+ getMD5Value(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加密
     * @param data
     * @return
     * @throws Exception
     */
    public String encode(String data) throws Exception {
        // 得到加密对象Cipher
        Cipher enCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        // 设置工作模式为加密模式，给出密钥和向量
        enCipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] pasByte = enCipher.doFinal(data.getBytes(this.charset));
        return Base64.getEncoder().encodeToString(pasByte);
    }

    /**
     * 解密
     * @param data
     * @return
     * @throws Exception
     */
    public String decode(String data) throws Exception {
        Cipher deCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        deCipher.init(Cipher.DECRYPT_MODE, key, iv);
        //此处注意doFinal()的参数的位数必须是8的倍数，否则会报错（通过encode加密的字符串读出来都是8的倍数位，但写入文件再读出来，就可能因为读取的方式的问题，导致最后此处的doFinal()的参数的位数不是8的倍数）
        //此处必须用base64Decoder，若用data。getBytes()则获取的字符串的byte数组的个数极可能不是8的倍数，而且不与上面的BASE64Encoder对应（即使解密不报错也不会得到正确结果）
        byte[] pasByte = deCipher.doFinal(Base64.getDecoder().decode(data));
        return new String(pasByte, this.charset);
    }

    /**
     * 获取MD5的值，可用于对比校验
     * @param sourceStr
     * @return
     */
    private static String getMD5Value(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
        }
        return result;
    }
}
