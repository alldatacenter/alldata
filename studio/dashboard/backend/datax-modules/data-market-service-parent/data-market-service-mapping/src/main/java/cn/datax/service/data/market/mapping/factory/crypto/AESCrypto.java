package cn.datax.service.data.market.mapping.factory.crypto;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESCrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator kGen = KeyGenerator.getInstance("AES");
            //2.根据 RULES 规则初始化密钥生成器，根据传入的字节数组生成一个128位的随机源
            kGen.init(128, new SecureRandom(SLAT.getBytes(CHARSET_UTF8)));
            //3.产生原始对称密钥
            SecretKey secretKey = kGen.generateKey();
            //4.获得原始对称密钥的字节数组
            byte[] enCodeFormat = secretKey.getEncoded();
            //5.根据字节数组生成AES密钥
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            //6.根据指定算法AES生成密码器
            Cipher cipher = Cipher.getInstance("AES");
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //8.根据密码器的初始化方式--加密：将数据加密
            byte[] AES_encrypt = cipher.doFinal(content.getBytes(CHARSET_UTF8));
            //9.将字符串返回
            return Base64.getEncoder().encodeToString(AES_encrypt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String decrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写
            KeyGenerator kGen = KeyGenerator.getInstance("AES");
            //2.根据 RULES 规则初始化密钥生成器，根据传入的字节数组生成一个128位的随机源
            kGen.init(128, new SecureRandom(SLAT.getBytes(CHARSET_UTF8)));
            //3.产生原始对称密钥
            SecretKey secretKey = kGen.generateKey();
            //4.获得原始对称密钥的字节数组
            byte[] enCodeFormat = secretKey.getEncoded();
            //5.根据字节数组生成AES密钥
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");
            //6.根据指定算法AES生成密码器
            Cipher cipher = Cipher.getInstance("AES");
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.DECRYPT_MODE, key);// 初始化
            //8.根据密码器的初始化方式--加密：将数据加密
            byte[] AES_decode = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(AES_decode, CHARSET_UTF8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
