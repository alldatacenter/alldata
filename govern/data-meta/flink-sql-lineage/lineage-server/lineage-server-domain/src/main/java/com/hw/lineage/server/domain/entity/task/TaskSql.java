package com.hw.lineage.server.domain.entity.task;

import com.hw.lineage.common.enums.SqlStatus;
import com.hw.lineage.common.enums.SqlType;
import com.hw.lineage.server.domain.vo.SqlId;
import com.hw.lineage.server.domain.vo.TaskId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: TaskSql
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class TaskSql {

    private SqlId sqlId;

    private TaskId taskId;

    /**
     * Base64 encode
     */
    private String sqlSource;

    private SqlType sqlType;

    private Long startLineNumber;

    private SqlStatus sqlStatus;

    private Boolean invalid;

}
