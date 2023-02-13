package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.util.Base64;

public class SHA1Crypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("SHA-1");
            md5.update(content.getBytes(CHARSET_UTF8));
            md5.update(SLAT.getBytes(CHARSET_UTF8));
        } catch (Exception e) {
        }
        return Base64.getEncoder().encodeToString(md5.digest());
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
