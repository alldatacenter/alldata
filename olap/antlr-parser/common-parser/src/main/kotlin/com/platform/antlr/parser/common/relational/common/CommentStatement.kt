package com.platform.antlr.parser.common.relational.common

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

data class CommentData(
    val comment: String? = null,
    val isNull: Boolean = false,
    val objType: String? = null,
    val objValue: String? = null
) : Statement() {
    override val statementType = StatementType.COMMENT
    override val privilegeType = PrivilegeType.ALTER
    override val sqlType = SqlType.TCL
}