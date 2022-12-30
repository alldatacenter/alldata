package cn.datax.service.data.market.mapping.factory;

import cn.datax.service.data.market.api.enums.AlgorithmCrypto;
import cn.datax.service.data.market.mapping.factory.crypto.AlgorithmRegistry;
import cn.datax.service.data.market.mapping.factory.crypto.Crypto;

public class AlgorithmFactory extends AbstractFactory {

    private static final AlgorithmRegistry ALGORITHM_REGISTRY = new AlgorithmRegistry();

    @Override
    public Crypto getCrypto(String type) {
        AlgorithmCrypto crypto = AlgorithmCrypto.getAlgorithmCrypto(type);
        return ALGORITHM_REGISTRY.getAlgorithm(crypto);
    }
}
