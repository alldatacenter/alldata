package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class CreateFileView(
    override val tableId: TableId,
    val path: String,
    var properties: Map<String, String>,
    var fileFormat: String? = null,
    val compression: String? = null,
    val sizeLimit: String? = null
) : AbsTableStatement() {
    override val statementType = StatementType.CREATE_FILE_VIEW
    override val privilegeType = PrivilegeType.OTHER
    override val sqlType = SqlType.DML
}