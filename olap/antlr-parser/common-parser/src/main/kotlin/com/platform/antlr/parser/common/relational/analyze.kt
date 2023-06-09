package com.platform.antlr.parser.common.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class AnalyzeTable (
    val tableIds: List<TableId>
) : Statement() {
    override val statementType = StatementType.ANALYZE_TABLE
    override val privilegeType = PrivilegeType.READ
    override val sqlType = SqlType.DML
}