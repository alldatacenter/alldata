package com.platform.antlr.parser.common.relational

import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.relational.table.ColumnRel
import kotlin.collections.ArrayList

data class AlterTable(
    val alterType: AlterType,
    override val tableId: TableId,
    private val action: AlterAction?,
    val tableType: TableType = TableType.TABLE
): AbsTableStatement() {
    override val statementType = StatementType.ALTER_TABLE
    override val privilegeType = PrivilegeType.ALTER
    override val sqlType = SqlType.DDL

    val actions: ArrayList<AlterAction> = ArrayList()
    var ifExists: Boolean = false

    init {
        if (action != null) {
            actions.add(action)
        }
    }

    constructor(alterType: AlterType, tableId: TableId): this(alterType, tableId, null)

    constructor(alterType: AlterType): this(alterType, TableId("__UNKOWN__"), null)

    fun addActions(list: List<AlterAction>) {
        actions.addAll(list)
    }

    fun firstAction(): AlterAction {
        return actions.first()
    }
}

abstract class AlterAction(val privilegeType: PrivilegeType = PrivilegeType.ALTER)

data class AlterTableAction(
    var newTableId: TableId? = null, // 修改表，新列名称
): AlterAction() {
    var location: String? = null
    var properties: Map<String, String>? = null
    var partitionVals: LinkedHashMap<String, String>? = null //spark touch
}

data class AlterViewAction(
    var querySql: String, // 修改表，新列名称
    var inputTables: List<TableId>,
    var functionNames: HashSet<String>
): AlterAction()

data class AlterColumnAction(
    var columName: String? = null, // 修改列名
    var dataType: String? = null,
    var comment: String? = null,
    var position: String? = null,
    var afterCol: String? = null,
    var setOrDrop: String? = null,
    var nullable: Boolean = true,
    var defaultExpression: String? = null,
    var dropDefault: Boolean = false,
): AlterAction() {
    var newColumName: String? = null // 修改列名，新列名称

    fun getColumn(): ColumnRel? {
        return if (columName != null) {
            ColumnRel(columName!!, dataType, comment, nullable, defaultExpression)
        } else {
            null
        }
    }
}

data class DropColumnAction(
    var columNames: ArrayList<String> = arrayListOf()
): AlterAction() {
    fun firstColumn(): String {
        return columNames.first()
    }

    constructor(columName: String): this(arrayListOf(columName))
}

data class AddPartitionAction(
    var ifNotExists: Boolean = false,
    var partitions: List<LinkedHashMap<String, String>>
): AlterAction()

data class DropPartitionAction(
    var ifExists: Boolean = false,
    var partitions: List<LinkedHashMap<String, String>>
): AlterAction()

data class RenamePartitionAction(
    var fromPartitionVals: LinkedHashMap<String, String>,
    var toPartitionVals: LinkedHashMap<String, String>
): AlterAction()

data class CreateIndex(
    val indexName: String,
    val indexColumnNames: ArrayList<IndexColumnName> = arrayListOf()
): AlterAction() {
    var intimeAction: String = "ONLINE" //mysql ONLINE & OFFLINE
    var indexCategory: String? = null
    var indexType: String? = null
    var comment: String? = null
}

data class IndexColumnName(
    val columnName: String,
    val sortType: SortType = SortType.UNKOWN,
)

data class DropIndex(
    val indexName: String,
    var ifExists: Boolean = false
): AlterAction()