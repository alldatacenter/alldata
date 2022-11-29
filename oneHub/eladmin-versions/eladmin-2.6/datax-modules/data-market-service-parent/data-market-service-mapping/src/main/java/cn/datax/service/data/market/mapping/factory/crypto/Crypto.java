package cn.datax.service.data.market.mapping.factory.crypto;

public interface Crypto {

    /**
     * 字符编码
     */
    String CHARSET_UTF8 = "UTF-8";
    /**
     * 密码盐
     */
    String SLAT = "DATAX:20200101";

    String encrypt(String content);

    String decrypt(String content);
}
