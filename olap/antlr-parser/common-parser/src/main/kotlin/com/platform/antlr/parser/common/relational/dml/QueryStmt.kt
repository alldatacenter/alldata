package com.platform.antlr.parser.common.relational.dml

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId

data class QueryStmt(
    var inputTables: List<TableId>,
    var limit: Int? = null,
    var offset: Int? = null
): Statement() {
    override val statementType = StatementType.SELECT
    override val privilegeType = PrivilegeType.READ
    override val sqlType = SqlType.DQL

    val functionNames: HashSet<String> = hashSetOf()
}