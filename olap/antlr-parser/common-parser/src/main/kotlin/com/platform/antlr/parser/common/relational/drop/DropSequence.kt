package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class DropSequence(
    override val tableId: TableId,
    var ifExists: Boolean = false,
    var isMaterialized: Boolean = false,
) : AbsTableStatement() {
    override val statementType = StatementType.DROP_SEQUENCE
    override val privilegeType = PrivilegeType.DROP
    override val sqlType = SqlType.DDL

    val tableIds: ArrayList<TableId> = arrayListOf()
}