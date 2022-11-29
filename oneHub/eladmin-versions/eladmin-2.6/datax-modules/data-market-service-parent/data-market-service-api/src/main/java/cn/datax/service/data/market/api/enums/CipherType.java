package cn.datax.service.data.market.api.enums;

public enum CipherType {

    REGEX("1", "正则替换"),
    ALGORITHM("2", "加密算法");

    private final String key;

    private final String val;

    CipherType(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    public static CipherType getCipherType(String cipherType) {
        for (CipherType type : CipherType.values()) {
            if (type.key.equals(cipherType)) {
                return type;
            }
        }
        return REGEX;
    }
}
