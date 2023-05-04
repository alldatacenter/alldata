package com.hw.security.flink.policy;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description: DataMaskPolicy
 * @author: HamaWhite
 */
@Data
@AllArgsConstructor
public class DataMaskPolicy {

    private String username;

    private String catalogName;

    private String database;

    private String tableName;

    private String columnName;

    private String condition;
}
