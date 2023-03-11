package com.hw.lineage.server.application.dto;

import com.hw.lineage.common.enums.SqlStatus;
import com.hw.lineage.common.enums.SqlType;
import lombok.Data;

import java.io.Serializable;

/**
 * @description: TaskSqlDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class TaskSqlDTO implements Serializable {

    private Long sqlId;

    private Long taskId;

    /**
     * Base64 encode
     */
    private String sqlSource;

    private SqlType sqlType;

    private Long startLineNumber;

    private SqlStatus sqlStatus;

}
