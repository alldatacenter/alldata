package cn.datax.service.data.market.api.enums;

public enum AlgorithmCrypto {

    BASE64("1", "BASE64加密"),
    MD5("2", "MD5加密"),
    SHA_1("3", "SHA_1加密"),
    SHA_256("4", "SHA_256加密"),
    AES("5", "AES加密"),
    DES("6", "DES加密");

    private final String key;

    private final String val;

    AlgorithmCrypto(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    public static AlgorithmCrypto getAlgorithmCrypto(String algorithmCrypt) {
        for (AlgorithmCrypto type : AlgorithmCrypto.values()) {
            if (type.key.equals(algorithmCrypt)) {
                return type;
            }
        }
        return BASE64;
    }
}
