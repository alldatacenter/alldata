package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

import java.util.Base64;

public class BASE64Crypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            byte[] encode = Base64.getEncoder().encode(content.getBytes(CHARSET_UTF8));
            return new String(encode, CHARSET_UTF8);
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
            byte[] decode = Base64.getDecoder().decode(content.getBytes(CHARSET_UTF8));
            return new String(decode, CHARSET_UTF8);
        } catch (Exception e) {
        }
        return null;
    }
}
