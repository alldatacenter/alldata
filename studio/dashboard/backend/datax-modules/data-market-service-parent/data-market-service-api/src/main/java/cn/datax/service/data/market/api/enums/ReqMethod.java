package cn.datax.service.data.market.api.enums;

public enum ReqMethod {

    GET("GET"),
    POST("POST");

    private String desc;

    ReqMethod(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
