package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class DESCrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            KeyGenerator kGen = KeyGenerator.getInstance("DES");
            kGen.init(56, new SecureRandom(SLAT.getBytes(CHARSET_UTF8)));
            SecretKey secretKey = kGen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] DES_encrypt = cipher.doFinal(content.getBytes(CHARSET_UTF8));
            return Base64.getEncoder().encodeToString(DES_encrypt);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public String decrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            KeyGenerator kGen = KeyGenerator.getInstance("DES");
            kGen.init(56, new SecureRandom(SLAT.getBytes(CHARSET_UTF8)));
            SecretKey secretKey = kGen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "DES");
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] DES_decrypt = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(DES_decrypt, CHARSET_UTF8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
