package com.platform.antlr.parser.common.relational

import org.apache.commons.lang3.StringUtils

data class TableId(
    val catalogName: String?,
    val schemaName: String?,
    val tableName: String,
    val metaType: String?) {

    constructor(catalogName: String?, schemaName: String?, tableName: String):
            this(catalogName, schemaName, tableName, null)

    constructor(schemaName: String?, tableName: String):
            this(null, schemaName, tableName, null)

    constructor(tableName: String):
            this(null, null, tableName, null)

    fun getFullTableName(): String {
        if (catalogName != null) {
            return "${catalogName}.${schemaName}.${tableName}"
        }

        if (schemaName != null) {
            return "${schemaName}.${tableName}"
        }

        return tableName
    }

    fun getLowerCatalogName(): String {
        return StringUtils.lowerCase(catalogName)
    }

    fun getLowerSchemaName(): String {
        return StringUtils.lowerCase(schemaName)
    }

    fun getLowerTableName(): String {
        return StringUtils.lowerCase(tableName)
    }
}