package cn.datax.service.data.masterdata.api.enums;

public enum OracleDataTypeEnum {

    CHAR("char", "字符串"), NUMBER("number", "数值"), DATE("date", "日期"), CLOB("clob", "长文本"), BLOB("blob", "二进制");

    private String value;
    private String title;

    OracleDataTypeEnum(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static OracleDataTypeEnum match(String value, OracleDataTypeEnum defaultItem) {
        if (value != null) {
            for (OracleDataTypeEnum item: OracleDataTypeEnum.values()) {
                if (item.getValue().equals(value)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
