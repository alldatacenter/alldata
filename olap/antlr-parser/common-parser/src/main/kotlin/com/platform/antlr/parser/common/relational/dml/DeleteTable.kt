package com.platform.antlr.parser.common.relational.dml

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class DeleteTable(
    override val tableId: TableId,
    val inputTables: List<TableId>
) : AbsTableStatement() {
    override val statementType = StatementType.DELETE
    override val privilegeType = PrivilegeType.WRITE
    override val sqlType = SqlType.DML

    val outputTables: ArrayList<TableId> = arrayListOf()
}
