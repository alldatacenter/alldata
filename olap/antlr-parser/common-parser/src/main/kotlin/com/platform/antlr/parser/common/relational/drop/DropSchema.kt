package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class DropSchema(
    val databaseName: String?,
    val schemaName: String,
): Statement() {
    override val statementType = StatementType.DROP_SCHEMA
    override val privilegeType = PrivilegeType.DROP
    override val sqlType = SqlType.DDL

    constructor(schemaName: String): this(null, schemaName)
}