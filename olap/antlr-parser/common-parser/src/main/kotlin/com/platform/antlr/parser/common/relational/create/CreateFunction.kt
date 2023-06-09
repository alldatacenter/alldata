package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.FunctionId
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId

data class CreateFunction(
    val functionId: FunctionId,
    var replace: Boolean = false,
    var temporary: Boolean = false,
    val className: String? = null,
    val file: String? = null
) : Statement() {
    override val statementType = StatementType.CREATE_FUNCTION
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    var inputTables: List<TableId> = listOf()

    constructor(functionId: FunctionId, replace: Boolean) : this(functionId, replace, false)
}
