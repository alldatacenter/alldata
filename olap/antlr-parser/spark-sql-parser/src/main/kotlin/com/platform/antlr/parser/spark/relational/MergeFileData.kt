package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class MergeFileData(
    override val tableId: TableId,
    var properties: Map<String, String>,
    var partitionVals: LinkedHashMap<String, String>
) : AbsTableStatement() {
    override val statementType = StatementType.MERGE_FILE
    override val privilegeType = PrivilegeType.WRITE
    override val sqlType = SqlType.DML
}