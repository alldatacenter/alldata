package com.platform.antlr.parser.common.relational.dml

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId

data class MergeTable(
    var targetTable: TableId,
    var inputTables: List<TableId> = listOf()
): Statement() {
    override val statementType = StatementType.MERGE
    override val privilegeType = PrivilegeType.WRITE
    override val sqlType = SqlType.DML
}