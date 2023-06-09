package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class CreateTempViewUsing(
    override val tableId: TableId,
    var fileFormat: String,
    var properties: Map<String, String>,
) : AbsTableStatement() {
    override val statementType = StatementType.CREATE_TEMP_VIEW_USING
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    var replace: Boolean = false
    var global: Boolean = false
}