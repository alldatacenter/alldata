package com.hw.lineage.common.result;

import com.hw.lineage.common.enums.TableKind;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * @description: TableResult
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TableResult {

    private String tableName;

    private TableKind tableKind;

    private String comment;

    private List<ColumnResult> columnList;

    private List<String> watermarkSpecList;

    /**
     * Properties of the table
     */
    private Map<String, String> propertiesMap;


}
