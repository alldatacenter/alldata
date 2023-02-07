package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [银行卡号] 前六位，后四位，其他用星号隐藏每位1个星号<例子:6222600**********1234>
 */
public class BANKCARDCrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.left(content, 6).concat(StringUtils.removeStart(StringUtils.leftPad(StringUtils.right(content, 4), StringUtils.length(content), "*"), "******"));
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
