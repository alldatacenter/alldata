package cn.datax.service.data.market.mapping.factory;

import cn.datax.service.data.market.api.enums.RegexCrypto;
import cn.datax.service.data.market.mapping.factory.crypto.Crypto;
import cn.datax.service.data.market.mapping.factory.crypto.RegexRegistry;

public class RegexFactory extends AbstractFactory {

    private static final RegexRegistry REGEX_REGISTRY = new RegexRegistry();

    @Override
    public Crypto getCrypto(String type) {
        RegexCrypto crypto = RegexCrypto.getRegexCrypto(type);
        return REGEX_REGISTRY.getRegex(crypto);
    }
}
