package com.hw.lineage.server.infrastructure.persistence.mapper;

import com.hw.lineage.common.enums.TaskStatus;
import com.hw.lineage.server.domain.graph.column.ColumnGraph;
import com.hw.lineage.server.domain.graph.table.TableGraph;
import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class TaskDynamicSqlSupport {
    public static final Task task = new Task();

    public static final SqlColumn<Long> taskId = task.taskId;

    public static final SqlColumn<Long> catalogId = task.catalogId;

    public static final SqlColumn<String> taskName = task.taskName;

    public static final SqlColumn<String> descr = task.descr;

    public static final SqlColumn<String> database = task.database;

    public static final SqlColumn<TaskStatus> taskStatus = task.taskStatus;

    public static final SqlColumn<Long> lineageTime = task.lineageTime;

    public static final SqlColumn<Long> createUserId = task.createUserId;

    public static final SqlColumn<Long> modifyUserId = task.modifyUserId;

    public static final SqlColumn<Long> createTime = task.createTime;

    public static final SqlColumn<Long> modifyTime = task.modifyTime;

    public static final SqlColumn<Boolean> invalid = task.invalid;

    /**
     * Base64 encode
     */
    public static final SqlColumn<String> taskSource = task.taskSource;

    public static final SqlColumn<String> taskLog = task.taskLog;

    public static final SqlColumn<TableGraph> tableGraph = task.tableGraph;

    public static final SqlColumn<ColumnGraph> columnGraph = task.columnGraph;

    public static final class Task extends AliasableSqlTable<Task> {
        public final SqlColumn<Long> taskId = column("`task_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> catalogId = column("`catalog_id`", JDBCType.BIGINT);

        public final SqlColumn<String> taskName = column("`task_name`", JDBCType.VARCHAR);

        public final SqlColumn<String> descr = column("`descr`", JDBCType.VARCHAR);

        public final SqlColumn<String> database = column("`database`", JDBCType.VARCHAR);

        public final SqlColumn<TaskStatus> taskStatus = column("`task_status`", JDBCType.TINYINT, "com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.TaskStatusTypeHandler");

        public final SqlColumn<Long> lineageTime = column("`lineage_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> createUserId = column("`create_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyUserId = column("`modify_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> createTime = column("`create_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyTime = column("`modify_time`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public final SqlColumn<String> taskSource = column("`task_source`", JDBCType.LONGVARCHAR);

        public final SqlColumn<String> taskLog = column("`task_log`", JDBCType.LONGVARCHAR);

        public final SqlColumn<TableGraph> tableGraph = column("`table_graph`", JDBCType.LONGVARCHAR, "com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.TableGraphTypeHandler");

        public final SqlColumn<ColumnGraph> columnGraph = column("`column_graph`", JDBCType.LONGVARCHAR, "com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.ColumnGraphTypeHandler");

        public Task() {
            super("bas_task", Task::new);
        }
    }
}