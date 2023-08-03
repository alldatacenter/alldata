package com.platform.antlr.parser.common.relational.table

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

class RepairTable(
    override val tableId: TableId
) : AbsTableStatement() {
    override val statementType = StatementType.REPAIR_TABLE
    override val privilegeType = PrivilegeType.ADMIN
    override val sqlType = SqlType.DDL
}