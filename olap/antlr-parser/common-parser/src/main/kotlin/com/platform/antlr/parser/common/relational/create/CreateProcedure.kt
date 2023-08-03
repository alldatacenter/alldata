package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.ProcedureId
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId

data class CreateProcedure(
    val procedureId: ProcedureId?,
    var replace: Boolean = false,
    var temporary: Boolean = false,
    val className: String? = null,
    val file: String? = null
) : Statement() {
    override val statementType = StatementType.CREATE_PROCEDURE
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    var inputTables: List<TableId> = listOf()
    var outputTables: List<TableId> = listOf()

    constructor(procedureId: ProcedureId, replace: Boolean) : this(procedureId, replace, false)

    constructor() : this(null)
}