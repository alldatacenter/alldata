package cn.datax.service.data.market.mapping.factory.crypto;

import cn.datax.service.data.market.api.enums.AlgorithmCrypto;

import java.util.EnumMap;
import java.util.Map;

public class AlgorithmRegistry {

    private final Map<AlgorithmCrypto, Crypto> algorithm_enum_map = new EnumMap<>(AlgorithmCrypto.class);

    public AlgorithmRegistry() {
        algorithm_enum_map.put(AlgorithmCrypto.BASE64, new BASE64Crypto());
        algorithm_enum_map.put(AlgorithmCrypto.AES, new AESCrypto());
        algorithm_enum_map.put(AlgorithmCrypto.DES, new DESCrypto());
        algorithm_enum_map.put(AlgorithmCrypto.MD5, new MD5Crypto());
        algorithm_enum_map.put(AlgorithmCrypto.SHA_1, new SHA1Crypto());
        algorithm_enum_map.put(AlgorithmCrypto.SHA_256, new SHA256Crypto());
    }

    public Crypto getAlgorithm(AlgorithmCrypto crypto) {
        return algorithm_enum_map.get(crypto);
    }
}
