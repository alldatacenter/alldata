package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class DropDatabase(
    val catalogName: String?,
    val databaseName: String,
    var ifExists: Boolean = false
): Statement() {
    override val statementType = StatementType.DROP_DATABASE
    override val privilegeType = PrivilegeType.DROP
    override val sqlType = SqlType.DDL

    constructor(databaseName: String): this(null, databaseName)
}