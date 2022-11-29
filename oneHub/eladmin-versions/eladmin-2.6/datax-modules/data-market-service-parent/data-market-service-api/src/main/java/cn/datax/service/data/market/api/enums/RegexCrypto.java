package cn.datax.service.data.market.api.enums;

public enum RegexCrypto {

    CHINESE_NAME("1", "中文姓名"),
    ID_CARD("2", "身份证号"),
    FIXED_PHONE("3", "固定电话"),
    MOBILE_PHONE("4", "手机号码"),
    ADDRESS("5", "地址"),
    EMAIL("6", "电子邮箱"),
    BANK_CARD("7", "银行卡号"),
    CNAPS_CODE("8", "公司开户银行联号");

    private final String key;

    private final String val;

    RegexCrypto(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }

    public static RegexCrypto getRegexCrypto(String regexCrypt) {
        for (RegexCrypto type : RegexCrypto.values()) {
            if (type.key.equals(regexCrypt)) {
                return type;
            }
        }
        return CHINESE_NAME;
    }
}
