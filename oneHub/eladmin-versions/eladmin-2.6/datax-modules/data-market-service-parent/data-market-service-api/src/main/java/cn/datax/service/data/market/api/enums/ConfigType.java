package cn.datax.service.data.market.api.enums;

public enum ConfigType {

    FORM("1", "表引导模式"),
    SCRIPT("2", "脚本模式");

    private final String key;

    private final String val;

    ConfigType(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public String getKey() {
        return key;
    }

    public String getVal() {
        return val;
    }
}
