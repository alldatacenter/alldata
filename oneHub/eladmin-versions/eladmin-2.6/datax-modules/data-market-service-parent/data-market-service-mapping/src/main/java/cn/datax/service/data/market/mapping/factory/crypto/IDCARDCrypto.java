package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [身份证号] 显示最后四位，其他隐藏。共计18位或者15位。<例子：*************5762>
 */
public class IDCARDCrypto implements Crypto {

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
