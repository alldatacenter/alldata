package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [中文姓名] 只显示第一个汉字，其他隐藏为星号<例子：李**>
 */
public class CHINESENAMECrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.rightPad(StringUtils.left(content, 1), StringUtils.length(content), "*");
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
