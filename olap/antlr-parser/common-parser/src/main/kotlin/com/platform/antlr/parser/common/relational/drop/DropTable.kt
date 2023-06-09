package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class DropTable(
    override val tableId: TableId,
    var ifExists: Boolean = false
) : AbsTableStatement() {
    override val statementType = StatementType.DROP_TABLE
    override val privilegeType = PrivilegeType.DROP
    override val sqlType = SqlType.DDL

    val tableIds: ArrayList<TableId> = arrayListOf()
    var force: Boolean = false

}