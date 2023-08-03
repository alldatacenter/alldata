package com.platform.antlr.parser.common.relational.common

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class UnSetStatement(
    val key: String
) : Statement() {
    override val statementType = StatementType.UNSET
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.TCL
}