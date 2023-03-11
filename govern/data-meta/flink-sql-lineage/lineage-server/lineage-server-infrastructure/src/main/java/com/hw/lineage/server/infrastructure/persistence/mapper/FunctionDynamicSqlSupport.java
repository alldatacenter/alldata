package com.hw.lineage.server.infrastructure.persistence.mapper;

import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class FunctionDynamicSqlSupport {
    public static final Function function = new Function();

    public static final SqlColumn<Long> functionId = function.functionId;

    public static final SqlColumn<Long> catalogId = function.catalogId;

    public static final SqlColumn<String> functionName = function.functionName;

    public static final SqlColumn<String> database = function.database;

    public static final SqlColumn<String> invocation = function.invocation;

    public static final SqlColumn<String> functionPath = function.functionPath;

    public static final SqlColumn<String> className = function.className;

    public static final SqlColumn<String> descr = function.descr;

    public static final SqlColumn<Long> createUserId = function.createUserId;

    public static final SqlColumn<Long> modifyUserId = function.modifyUserId;

    public static final SqlColumn<Long> createTime = function.createTime;

    public static final SqlColumn<Long> modifyTime = function.modifyTime;

    public static final SqlColumn<Boolean> invalid = function.invalid;

    public static final class Function extends AliasableSqlTable<Function> {
        public final SqlColumn<Long> functionId = column("`function_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> catalogId = column("`catalog_id`", JDBCType.BIGINT);

        public final SqlColumn<String> functionName = column("`function_name`", JDBCType.VARCHAR);

        public final SqlColumn<String> database = column("`database`", JDBCType.VARCHAR);

        public final SqlColumn<String> invocation = column("`invocation`", JDBCType.VARCHAR);

        public final SqlColumn<String> functionPath = column("`function_path`", JDBCType.VARCHAR);

        public final SqlColumn<String> className = column("`class_name`", JDBCType.VARCHAR);

        public final SqlColumn<String> descr = column("`descr`", JDBCType.VARCHAR);

        public final SqlColumn<Long> createUserId = column("`create_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyUserId = column("`modify_user_id`", JDBCType.BIGINT);

        public final SqlColumn<Long> createTime = column("`create_time`", JDBCType.BIGINT);

        public final SqlColumn<Long> modifyTime = column("`modify_time`", JDBCType.BIGINT);

        public final SqlColumn<Boolean> invalid = column("`invalid`", JDBCType.BIT);

        public Function() {
            super("bas_function", Function::new);
        }
    }
}