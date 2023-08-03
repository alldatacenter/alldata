package com.platform.antlr.parser.common

import java.io.Serializable

enum class AlterType : Serializable {
    SET_TABLE_LOCATION,
    SET_TABLE_PROPERTIES,
    TOUCH_TABLE,

    ALTER_COLUMN,
    ALTER_VIEW,

    ADD_PARTITION,
    ADD_COLUMN,
    ADD_INDEX,
    ADD_UNIQUE_KEY,
    ADD_PRIMARY_KEY,

    DROP_PARTITION,
    DROP_COLUMN,
    DROP_INDEX,

    RENAME_COLUMN,
    RENAME_PARTITION,
    RENAME_TABLE,

    DETACH_PARTITION,
    ATTACH_PARTITION,
    TRUNCATE_PARTITION,

    UNKOWN
}