package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class CallProcedure(
    val catalogName: String?,
    val databaseName: String?,
    val procedureName: String,
    var properties: Map<String, String>
) : Statement() {
    override val statementType = StatementType.CALL
    override val privilegeType = PrivilegeType.ADMIN
    override val sqlType = SqlType.DML
}