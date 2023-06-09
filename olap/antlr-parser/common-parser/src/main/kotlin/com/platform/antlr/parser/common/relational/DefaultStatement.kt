package com.platform.antlr.parser.common.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement

class DefaultStatement(override val statementType: StatementType): Statement() {
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}