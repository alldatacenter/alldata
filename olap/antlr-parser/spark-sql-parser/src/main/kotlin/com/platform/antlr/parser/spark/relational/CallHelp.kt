package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class CallHelp(
    val procedureName: String?
) : Statement() {
    override val statementType = StatementType.HELP
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}