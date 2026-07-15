package com.platform.admin.tool.database;

import lombok.Data;

import java.util.List;

/**
 * 表信息
 */
@Data
public class TableInfo {
    /**
     * 表名
     */
    private String name;

    /**
     * 注释
     */
    private String comment;
    /**
     * 所有列
     */
    private List<ColumnInfo> columns;
}
