package com.platform.antlr.parser.common.relational

import org.apache.commons.lang3.StringUtils

data class FunctionId(
    val catalogName: String?,
    val schemaName: String?,
    val functionName: String) {

    constructor(schemaName: String?, tableName: String):
            this(null, schemaName, tableName)

    constructor(tableName: String):
            this(null, null, tableName)

    fun getFullFunctionName(): String {
        if (catalogName != null) {
            return "${catalogName}.${schemaName}.${functionName}"
        }

        if (schemaName != null) {
            return "${schemaName}.${functionName}"
        }

        return functionName
    }

    fun getLowerCatalogName(): String {
        return StringUtils.lowerCase(catalogName)
    }

    fun getLowerSchemaName(): String {
        return StringUtils.lowerCase(schemaName)
    }

    fun getLowerTableName(): String {
        return StringUtils.lowerCase(functionName)
    }
}