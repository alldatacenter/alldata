package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class CreateTableLike(
    val oldTableId: TableId,
    override val tableId: TableId,
    var ifNotExists: Boolean = false,
    var external: Boolean = false,
    var temporary: Boolean = false
) : AbsTableStatement() {
    override val statementType = StatementType.CREATE_TABLE_AS_LIKE
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL
}