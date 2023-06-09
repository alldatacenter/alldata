package com.platform.antlr.parser.common.relational.dml

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class InsertTable(
    val mode: InsertMode,
    override val tableId: TableId
): AbsTableStatement() {
    override val statementType = StatementType.INSERT
    override val privilegeType = PrivilegeType.WRITE
    override val sqlType = SqlType.DML

    val inputTables: ArrayList<TableId> = arrayListOf()
    val outputTables: ArrayList<TableId> = arrayListOf()
    val functionNames: HashSet<String> = hashSetOf()

    var properties: Map<String, String>? = null
    var fileFormat: String? = null
    var partitionVals: LinkedHashMap<String, String>? = null
    var querySql: String? = null
    var rows: ArrayList<List<String>>? = null

    var mysqlReplace: Boolean = false
}