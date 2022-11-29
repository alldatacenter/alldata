package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [固定电话] 后四位，其他隐藏<例子：****1234>
 */
public class FIXEDPHONECrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.leftPad(StringUtils.right(content, 4), StringUtils.length(content), "*");
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
