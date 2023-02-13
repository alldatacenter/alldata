package cn.datax.service.data.metadata.api.enums;

public enum DataLevel {

    DATABASE("database", 1),
    TABLE("table", 2),
    COLUMN("column", 3);

    private final String key;

    private final Integer level;

    DataLevel(String key, Integer level) {
        this.key = key;
        this.level = level;
    }

    public String getKey() {
        return key;
    }

    public Integer getLevel() {
        return level;
    }

    public static DataLevel getLevel(String key) {
        for (DataLevel type : DataLevel.values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return DATABASE;
    }
}
