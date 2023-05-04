package com.hw.security.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: ColumnEntity
 * @author: HamaWhite
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class ColumnEntity {

    private String columnName;

    private String columnType;
}
