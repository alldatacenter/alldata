package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class SyncSchemaExpr(
    val targetCatalogName: String?,
    val targetDatabaseName: String,
    val sourceCatalogName: String?,
    val sourceDatabaseName: String,
    val owner: String?
) : Statement() {
    override val statementType = StatementType.SYNC
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DDL
}