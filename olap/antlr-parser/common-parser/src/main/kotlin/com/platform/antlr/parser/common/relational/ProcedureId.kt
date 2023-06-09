package com.platform.antlr.parser.common.relational

import org.apache.commons.lang3.StringUtils

data class ProcedureId(
    val catalogName: String?,
    val schemaName: String?,
    val procedureName: String) {

    constructor(schemaName: String?, tableName: String):
            this(null, schemaName, tableName)

    constructor(tableName: String):
            this(null, null, tableName)

    fun getFullFunctionName(): String {
        if (catalogName != null) {
            return "${catalogName}.${schemaName}.${procedureName}"
        }

        if (schemaName != null) {
            return "${schemaName}.${procedureName}"
        }

        return procedureName
    }

    fun getLowerCatalogName(): String {
        return StringUtils.lowerCase(catalogName)
    }

    fun getLowerSchemaName(): String {
        return StringUtils.lowerCase(schemaName)
    }

    fun getLowerTableName(): String {
        return StringUtils.lowerCase(procedureName)
    }
}