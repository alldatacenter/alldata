package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class TaskLineageDynamicSqlSupport {
    public static final TaskLineage taskLineage = new TaskLineage();

    public static final SqlColumn<Long> rid = taskLineage.rid;

    public static final SqlColumn<Long> taskId = taskLineage.taskId;

    public static final SqlColumn<Long> sqlId = taskLineage.sqlId;

    public static final SqlColumn<String> sourceCatalog = taskLineage.sourceCatalog;

    public static final SqlColumn<String> sourceDatabase = taskLineage.sourceDatabase;

    public static final SqlColumn<String> sourceTable = taskLineage.sourceTable;

    public static final SqlColumn<String> sourceColumn = taskLineage.sourceColumn;

    public static final SqlColumn<String> targetCatalog = taskLineage.targetCatalog;

    public static final SqlColumn<String> targetDatabase = taskLineage.targetDatabase;

    public static final SqlColumn<String> targetTable = taskLineage.targetTable;

    public static final SqlColumn<String> targetColumn = taskLineage.targetColumn;

    public static final SqlColumn<String> transform = taskLineage.transform;

    public static final SqlColumn<Boolean> invalid = taskLineage.invalid;

    public static final class TaskLineage extends AliasableSqlTable<TaskLineage> {
        public final SqlColumn<Long> rid = column("`rid`", JDBCType.BIGINT);

        public final SqlColumn<Long> taskId = column("`task_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> sqlId = column("`sql_id`", JDBCType.BIGINT);

        public final SqlColumn<String> sourceCatalog = column("`source_catalog`", JDBCType.VARCHAR);

        public final SqlColumn<String> sourceDatabase = column("`source_database`", JDBCType.VARCHAR);

        public final SqlColumn<String> sourceTable = column("`source_table`", JDBCType.VARCHAR);

        public final SqlColumn<String> sourceColumn = column("`source_column`", JDBCType.VARCHAR);

        public final SqlColumn<String> targetCatalog = column("`target_catalog`", JDBCType.VARCHAR);

        public final SqlColumn<String> targetDatabase = column("`target_database`", JDBCType.VARCHAR);

        public final SqlColumn<String> targetTable = column("`target_table`", JDBCType.VARCHAR);

        public final SqlColumn<String> targetColumn = column("`target_column`", JDBCType.VARCHAR);

        public final SqlColumn<String> transform = column("`transform`", JDBCType.VARCHAR);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public TaskLineage() {
            super("rel_task_lineage", TaskLineage::new);
        }
    }
}