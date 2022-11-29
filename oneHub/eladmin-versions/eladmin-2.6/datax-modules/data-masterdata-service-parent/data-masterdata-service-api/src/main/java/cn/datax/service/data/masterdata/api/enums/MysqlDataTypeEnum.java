package cn.datax.service.data.masterdata.api.enums;

public enum MysqlDataTypeEnum {

    TINYINT("tinyint", "tinyint整型"), INT("int", "int整型"), BIGINT("bigint", "bigint整型"),
    FLOAT("float", "单精度"), DOUBLE("double", "双精度"), DECIMAL("decimal", "定点数"),
    CHAR("char", "定长字符串"), VARCHAR("varchar", "变长字符串"), TEXT("text", "长文本"),
    DATE("date", "date日期"), TIME("time", "time日期"), YEAR("year", "year日期"), DATETIME("datetime", "datetime日期"),
    BLOB("blob", "二进制");

    private String value;
    private String title;

    MysqlDataTypeEnum(String value, String title) {
        this.value = value;
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static MysqlDataTypeEnum match(String value, MysqlDataTypeEnum defaultItem) {
        if (value != null) {
            for (MysqlDataTypeEnum item: MysqlDataTypeEnum.values()) {
                if (item.getValue().equals(value)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
