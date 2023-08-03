package com.platform.antlr.parser.common

import java.io.Serializable

/**
 * Created by libinsong on 2017/3/6.
 */

enum class StatementType: Serializable {
    CREATE_CATALOG,
    CREATE_DATABASE,
    CREATE_SCHEMA,
    CREATE_TABLE,
    CREATE_TABLE_AS_SELECT,
    CREATE_TABLE_AS_LIKE,
    CREATE_MATERIALIZED_VIEW,
    CREATE_VIEW,
    CREATE_FILE_VIEW, // spark
    CREATE_TEMP_VIEW_USING, //spark
    CREATE_FUNCTION,
    CREATE_PROCEDURE,

    DROP_CATALOG,
    DROP_DATABASE,
    DROP_SCHEMA,
    DROP_TABLE,
    DROP_VIEW,
    DROP_MATERIALIZED_VIEW,
    DROP_FUNCTION,
    DROP_SEQUENCE,
    DROP_PROCEDURE,

    TRUNCATE_TABLE,
    REFRESH_TABLE,
    EXPORT_TABLE,
    ANALYZE_TABLE,

    ALTER_TABLE,
    REPAIR_TABLE,
    COMMENT,

    //DML
    SELECT,
    DELETE,
    UPDATE,
    MERGE,
    INSERT,

    SHOW,
    DESC,

    CACHE,
    UNCACHE,
    CLEAR_CACHE,

    EXPLAIN,
    SET,
    UNSET,
    USE,

    DATATUNNEL, // spark
    MERGE_FILE, // spark
    APP_JAR, // spark
    CALL, // hudi
    HELP, // hudi
    SYNC,

    ARITHMETIC,

    FLINK_CDC_BEGIN,
    FLINK_CDC_END,
    FLINK_CDC_CTAS,
    FLINK_CDC_CDAS,

    UNKOWN;
}
