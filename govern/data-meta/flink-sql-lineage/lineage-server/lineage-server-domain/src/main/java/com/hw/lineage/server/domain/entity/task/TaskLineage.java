package com.hw.lineage.server.domain.entity.task;

import com.hw.lineage.server.domain.vo.SqlId;
import com.hw.lineage.server.domain.vo.TaskId;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.hw.lineage.common.util.Constant.DELIMITER;

/**
 * @description: TaskLineage
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class TaskLineage {

    private TaskId taskId;

    private SqlId sqlId;

    private String sourceCatalog;

    private String sourceDatabase;

    private String sourceTable;

    private String sourceColumn;

    private String targetCatalog;

    private String targetDatabase;

    private String targetTable;

    private String targetColumn;

    private String transform;

    private Boolean invalid;

    public String buildSourceTableName() {
        return String.join(DELIMITER, sourceCatalog, sourceDatabase, sourceTable);
    }

    public String buildTargetTableName() {
        return String.join(DELIMITER, targetCatalog, targetDatabase, targetTable);
    }

    public String buildSourceColumnName() {
        return String.join(DELIMITER, buildSourceTableName(), sourceColumn);
    }

    public String buildTargetColumnName() {
        return String.join(DELIMITER, buildTargetTableName(), targetColumn);
    }

}
