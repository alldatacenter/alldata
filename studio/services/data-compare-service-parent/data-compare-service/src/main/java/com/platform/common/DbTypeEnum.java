package com.platform.common;

public enum DbTypeEnum {
    MySQL(0, "MySQL", "com.mysql.cj.jdbc.Driver"),
    Hive(1, "Hive", "org.apache.hive.jdbc.HiveDriver"),
    Doris(2, "Doris", "com.mysql.cj.jdbc.Driver");

    private int i;
    private String type;
    private String connectDriver;

    DbTypeEnum(int i, String type, String connectDriver) {
        this.i = i;
        this.type = type;
        this.connectDriver = connectDriver;
    }


    public static DbTypeEnum findEnumByType(String type) {
        for (DbTypeEnum dbTypeEnum : DbTypeEnum.values()) {
            if (dbTypeEnum.getType().equals(type)) {
                return dbTypeEnum;
            }
        }
        throw new IllegalArgumentException("name is invalid");
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnectDriver() {
        return connectDriver;
    }

    public void setConnectDriver(String connectDriver) {
        this.connectDriver = connectDriver;
    }
}
