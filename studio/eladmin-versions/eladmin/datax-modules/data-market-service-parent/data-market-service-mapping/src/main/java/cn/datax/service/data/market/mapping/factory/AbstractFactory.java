package cn.datax.service.data.market.mapping.factory;

import cn.datax.service.data.market.mapping.factory.crypto.Crypto;

public abstract class AbstractFactory {

    public abstract Crypto getCrypto(String type);
}
