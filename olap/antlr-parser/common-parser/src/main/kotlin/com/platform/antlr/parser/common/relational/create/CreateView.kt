package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.table.ColumnRel

data class CreateView(
    override val tableId: TableId,
    var querySql: String? = null,
    val comment: String? = null,
    var ifNotExists: Boolean = false, //是否存在 if not exists 关键字
    var columnRels: List<ColumnRel>? = null
) : AbsTableStatement() {
    override val statementType = StatementType.CREATE_VIEW
    override val privilegeType = PrivilegeType.CREATE
    override val sqlType = SqlType.DDL

    var replace: Boolean = false
    var temporary: Boolean = false
    var global: Boolean = false

    var fileFormat: String? = null
    var properties: Map<String, String> = mapOf()

    val inputTables: ArrayList<TableId> = arrayListOf()
    val functionNames: HashSet<String> = hashSetOf()
}
