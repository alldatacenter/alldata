package com.hw.security.flink.policy;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description: RowFilterPolicy
 * @author: HamaWhite
 */
@Data
@AllArgsConstructor
public class RowFilterPolicy {

    private String username;

    private String catalogName;

    private String database;

    private String tableName;

    private String condition;
}
