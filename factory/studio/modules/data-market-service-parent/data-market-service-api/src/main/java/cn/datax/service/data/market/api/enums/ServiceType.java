package cn.datax.service.data.market.api.enums;

public enum ServiceType {

    HTTP("1", "http接口"),
    WEBSERVICE("2", "webservice接口");

    private final String key;

    private final String val;

    ServiceType(String key, String val) {
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
