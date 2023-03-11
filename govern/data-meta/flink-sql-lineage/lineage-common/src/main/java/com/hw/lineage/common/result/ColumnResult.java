package com.hw.lineage.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @description: ColumnResult
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ColumnResult {

    private String columnName;

    private String columnType;

    private String comment;

    private Boolean primaryKey = false;
}
