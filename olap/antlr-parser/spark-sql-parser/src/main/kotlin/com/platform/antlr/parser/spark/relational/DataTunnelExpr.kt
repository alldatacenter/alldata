package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId

data class DataTunnelExpr(
    val srcType: String,
    var srcOptions: Map<String, Any>,
    val transformSql: String?,
    val distType: String,
    var distOptions: Map<String, Any>
) : Statement() {
    override val statementType = StatementType.DATATUNNEL
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML

    val inputTables: ArrayList<TableId> = arrayListOf()
    val functionNames: HashSet<String> = hashSetOf()
}