package com.platform.antlr.parser.spark.relational

import com.platform.antlr.parser.common.PrivilegeType
import com.platform.antlr.parser.common.SqlType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.AbsTableStatement
import com.platform.antlr.parser.common.relational.TableId

data class ExportData(
    override val tableId: TableId,
    val path: String,
    var properties: Map<String, String>,
    var partitionVals: LinkedHashMap<String, String>,
    var fileFormat: String? = null,
    var compression: String? = null,
    var maxFileSize: String? = null,
    var overwrite: Boolean = false,
    var single: Boolean = false,
    var inputTables: ArrayList<TableId>
) : AbsTableStatement() {
    override val statementType = StatementType.EXPORT_TABLE
    override val privilegeType = PrivilegeType.READ
    override val sqlType = SqlType.DML

    val functionNames: HashSet<String> = hashSetOf()

    constructor(
        tableId: TableId,
        path: String,
        properties: Map<String, String>,
        partitionVals: LinkedHashMap<String, String>,
        fileFormat: String? = null,
        compression: String? = null,
        maxFileSize: String? = null,
        overwrite: Boolean = false,
        single: Boolean = false,):
            this(tableId, path, properties, partitionVals, fileFormat, compression, maxFileSize, overwrite, single, arrayListOf())
}