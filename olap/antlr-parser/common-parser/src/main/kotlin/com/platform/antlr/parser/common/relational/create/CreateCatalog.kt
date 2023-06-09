package com.platform.antlr.parser.common.relational.create

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class CreateCatalog(
    val catalogName: String,
    var properties: Map<String, String>? = null,
): Statement() {
    override val statementType = StatementType.CREATE_CATALOG
    override val privilegeType = PrivilegeType.ADMIN
    override val sqlType = SqlType.DDL

    constructor(catalogName: String): this(catalogName, null)
}