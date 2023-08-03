package com.platform.antlr.parser.common.util

import org.apache.commons.lang3.StringUtils

/**
 * Created by libinsong on 2017/4/10.
 */
object StringUtil {

    fun cleanQuote(value: String) : String {
        var result = value;
        if (StringUtils.startsWith(result, "'") && StringUtils.endsWith(result, "'")) {
            result = StringUtils.substring(result, 1, -1)
        }

        if (StringUtils.startsWith(result, "\"") && StringUtils.endsWith(result, "\"")) {
            result = StringUtils.substring(result, 1, -1)
        }

        if (StringUtils.startsWith(value, "`") && StringUtils.endsWith(value, "`")) {
            return StringUtils.substring(value, 1, -1)
        }
        return result
    }

    fun parseDataType(type: String): String {
        val value = if (StringUtils.startsWith(type, "TOK_")) StringUtils.substringAfter(type, "TOK_") else type
        return StringUtils.lowerCase(value)
    }

    fun innerFullTableName(catalogName: String?, databaseName: String?, tableName: String): String {
        if (catalogName != null) {
            return "${catalogName}.${databaseName}.${tableName}"
        }

        if (databaseName != null) {
            return "${databaseName}.${tableName}"
        }

        return tableName
    }
}
