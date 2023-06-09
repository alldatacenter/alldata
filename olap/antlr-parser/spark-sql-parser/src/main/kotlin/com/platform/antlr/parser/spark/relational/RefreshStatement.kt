package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class RefreshStatement(
    override val tableId: TableId
) : AbsTableStatement() {
    override val statementType = StatementType.REFRESH_TABLE
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.TCL
}