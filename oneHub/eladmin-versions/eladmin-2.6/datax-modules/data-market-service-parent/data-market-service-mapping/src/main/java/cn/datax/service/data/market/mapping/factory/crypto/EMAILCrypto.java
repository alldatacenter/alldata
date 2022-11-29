package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [电子邮箱] 只显示前三后显示邮箱后缀，其他隐藏为星号<例子：312****@qq.com>
 */
public class EMAILCrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return content.replaceAll("(\\w{3}).*@(\\w+)", "$1****@$2");
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
