package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [地址] 只显示前六位，不显示详细地址；我们要对个人信息增强保护<例子：北京市海淀区****>
 */
public class ADDRESSCrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.rightPad(StringUtils.left(content, 6), StringUtils.length(content), "*");
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
