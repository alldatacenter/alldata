package com.hw.lineage.server.application.dto;

import com.hw.lineage.server.application.dto.basic.BasicDTO;
import com.hw.lineage.server.application.dto.graph.LineageGraph;
import lombok.Data;

import java.util.List;

/**
 * @description: TaskDto
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class TaskDTO extends BasicDTO {
    private Long taskId;

    private Long catalogId;

    private String taskName;

    private String descr;

    private String database;

    private String taskSource;

    private String taskLog;

    private Long lineageTime;

    private List<TaskSqlDTO> taskSqlList;

    private LineageGraph lineageGraph;
}
