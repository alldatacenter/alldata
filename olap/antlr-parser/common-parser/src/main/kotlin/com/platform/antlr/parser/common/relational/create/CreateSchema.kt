package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class CreateSchema(
    val databaseName: String?,
    val schemaName: String
): Statement() {
    override val statementType = StatementType.CREATE_SCHEMA
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    constructor(schemaName: String): this(null, schemaName)
}