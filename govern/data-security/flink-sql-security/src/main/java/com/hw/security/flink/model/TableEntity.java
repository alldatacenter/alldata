package com.hw.security.flink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description: TableEntity
 * @author: HamaWhite
 */
@Data
@AllArgsConstructor
@Accessors(chain = true)
public class TableEntity {

    private String tableName;

    private List<ColumnEntity> columnList;
}
