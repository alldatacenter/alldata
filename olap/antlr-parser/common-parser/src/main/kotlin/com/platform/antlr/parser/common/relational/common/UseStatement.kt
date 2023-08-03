package com.platform.antlr.parser.common.relational.common

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class UseCatalog(
    val catalogName: String
) : Statement() {
    override val statementType = StatementType.USE
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}

data class UseDatabase(
    val catalogName: String?,
    val databaseName: String
) : Statement() {
    override val statementType = StatementType.USE
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML

    constructor(databaseName: String): this(null, databaseName)
}

data class UseSchema(
    val databaseName: String?,
    val schemaName: String
) : Statement() {
    override val statementType = StatementType.USE
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML

    constructor(databaseName: String): this(null, databaseName)
}