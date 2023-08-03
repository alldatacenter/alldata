package com.platform.antlr.parser.appjar

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class AppJarInfo(
    val resourceName: String,
    val className: String,
    val params: List<String>?
) : Statement() {
    override val statementType = StatementType.APP_JAR
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}