package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [手机号码] 前三位，后四位，其他隐藏<例子:138******1234>
 */
public class MOBILEPHONECrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.left(content, 3).concat(StringUtils.removeStart(StringUtils.leftPad(StringUtils.right(content, 4), StringUtils.length(content), "*"), "***"));
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
