package com.platform.antlr.parser.common.relational.drop

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class DropCatalog(
    val catalogName: String
): Statement() {
    override val statementType = StatementType.DROP_CATALOG
    override val privilegeType = PrivilegeType.ADMIN
    override val sqlType = SqlType.DDL
}