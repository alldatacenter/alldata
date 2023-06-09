package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class CreateDatabase(
    val catalogName: String?,
    val databaseName: String,
    val location: String? = null,
    var properties: Map<String, String>? = null,
    var ifNotExists: Boolean = false,
): Statement() {
    override val statementType = StatementType.CREATE_DATABASE
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    constructor(databaseName: String): this(null, databaseName, null, null)

    constructor(catalogName: String?, databaseName: String, properties: Map<String, String>? = null, ifNotExists: Boolean = false):
            this(catalogName, databaseName, null, properties, ifNotExists)
}