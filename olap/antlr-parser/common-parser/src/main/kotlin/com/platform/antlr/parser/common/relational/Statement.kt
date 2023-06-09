package com.platform.antlr.parser.common.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import java.io.Serializable

abstract class Statement: Serializable {
    abstract val statementType: StatementType
    abstract val privilegeType: PrivilegeType
    abstract val sqlType: SqlType
}