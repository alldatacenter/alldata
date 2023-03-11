package com.hw.lineage.common.enums;


import com.hw.lineage.common.enums.basic.StringEnum;

/**
 * @description: CatalogType
 * @author: HamaWhite
 * @version: 1.0.0
 */
public enum CatalogType implements StringEnum<CatalogType> {

    MEMORY("generic_in_memory"),
    HIVE("hive"),
    JDBC("jdbc");

    private final String value;

    CatalogType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
