package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class DropFunction(
    val schemaName: String?,
    val funcName: String
) : Statement() {
    override val statementType = StatementType.DROP_FUNCTION
    override val privilegeType = PrivilegeType.DROP
    override val sqlType = SqlType.DDL
}
