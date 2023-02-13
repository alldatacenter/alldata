package com.platform.admin.tool.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 原始jdbc字段对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DasColumn {

    private String columnName;

    private String columnTypeName;

    private String columnClassName;

    private String columnComment;
    private int isNull;
    private boolean isprimaryKey;
}
