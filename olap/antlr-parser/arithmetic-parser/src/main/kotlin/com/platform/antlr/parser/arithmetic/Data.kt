package com.platform.antlr.parser.arithmetic

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.Statement
import java.util.*

data class ArithmeticData(
    val variables: java.util.HashSet<String> = HashSet(),
    val functions: java.util.HashSet<String> = HashSet()
): Statement() {
    override val statementType = StatementType.ARITHMETIC
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}