package cn.datax.service.data.market.mapping.factory.crypto;

import org.apache.commons.lang3.StringUtils;

/**
 * [公司开户银行联号] 公司开户银行联行号,显示前四位，其他用星号隐藏，每位1个星号<例子:1234********>
 */
public class CNAPSCODECrypto implements Crypto {

    @Override
    public String encrypt(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return StringUtils.rightPad(StringUtils.left(content, 4), StringUtils.length(content), "*");
    }

    @Override
    public String decrypt(String content) {
        return null;
    }
}
