package com.hw.lineage.server.domain.entity.task;

import com.hw.lineage.common.enums.TaskStatus;
import com.hw.lineage.server.domain.entity.basic.BasicEntity;
import com.hw.lineage.server.domain.graph.column.ColumnGraph;
import com.hw.lineage.server.domain.graph.table.TableGraph;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.CatalogId;
import com.hw.lineage.server.domain.vo.TaskId;
import com.hw.lineage.server.domain.vo.TaskSource;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: Task
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Task extends BasicEntity implements Entity {
    private TaskId taskId;

    private CatalogId catalogId;

    private String taskName;

    private String descr;

    private String database;

    private TaskSource taskSource;

    private TaskStatus taskStatus;

    private String taskLog;

    private TableGraph tableGraph;

    private ColumnGraph columnGraph;

    private Long lineageTime;

    private List<TaskSql> taskSqlList;

    private List<TaskLineage> taskLineageList;

    public Task() {
        this.taskSqlList = new ArrayList<>();
        this.taskLineageList = new ArrayList<>();
    }

    public boolean addTaskSql(TaskSql taskSql) {
        return this.taskSqlList.add(taskSql);
    }

    public boolean addTaskLineage(TaskLineage taskLineage) {
        return this.taskLineageList.add(taskLineage);
    }
}
