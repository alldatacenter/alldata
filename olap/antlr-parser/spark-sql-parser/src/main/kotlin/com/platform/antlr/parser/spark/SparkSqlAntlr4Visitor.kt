package com.platform.antlr.parser.spark

import com.platform.antlr.parser.common.util.StringUtil
import com.platform.antlr.parser.spark.relational.*
import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.AlterType.*
import com.platform.antlr.parser.common.relational.*
import com.platform.antlr.parser.spark.relational.RefreshStatement
import com.platform.antlr.parser.common.relational.common.UseCatalog
import com.platform.antlr.parser.common.relational.common.UseDatabase
import com.platform.antlr.parser.common.relational.create.*
import com.platform.antlr.parser.common.relational.create.CreateView
import com.platform.antlr.parser.common.relational.dml.*
import com.platform.antlr.parser.common.relational.drop.*
import com.platform.antlr.parser.common.relational.table.*
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.AlterColumnActionContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.ColDefinitionOptionContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.CreateOrReplaceTableColTypeListContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.CreateTableClausesContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.DtColPropertyContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.DtPropertyContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.DtPropertyListContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.IdentifierContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.InsertIntoContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.PartitionSpecContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.PropertyListContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.QueryContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.SingleInsertQueryContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser.TableProviderContext
import com.platform.antlr.parser.spark.antlr4.SparkSqlParserBaseVisitor
import org.antlr.v4.runtime.tree.RuleNode
import org.apache.commons.lang3.StringUtils
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class SparkSqlAntlr4Visitor : SparkSqlParserBaseVisitor<Statement>() {

    private var currentOptType: StatementType = StatementType.UNKOWN
    private var currentAlterType: AlterType = UNKOWN
    private var multiInsertToken: String? = null

    private var limit: Int? = null
    private var offset: Int? = null
    private var inputTables: ArrayList<TableId> = arrayListOf()
    private var outputTables: ArrayList<TableId> = arrayListOf()
    private var cteTempTables: ArrayList<TableId> = arrayListOf()
    private var functionNames: HashSet<String> = hashSetOf()

    private var command: String? = null
    private var rows: ArrayList<List<String>> = ArrayList()

    private var insertSql: Boolean = false;

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Statement?): Boolean {
        return if (currentResult == null) true else false
    }

    override fun visitSingleStatement(ctx: SparkSqlParser.SingleStatementContext): Statement {
        val data = super.visitSingleStatement(ctx)

        if (data == null) {
            val startToken = StringUtils.lowerCase(ctx.getStart().text)
            return if ("show".equals(startToken)) {
                DefaultStatement(StatementType.SHOW)
            } else if ("desc".equals(startToken) || "describe".equals(startToken)) {
                DefaultStatement(StatementType.DESC)
            } else {
                throw SQLParserException("不支持的SQL: " + command)
            }
        }

        return data
    }

    fun setCommand(command: String) {
        this.command = command
    }

    fun parseNamespace(ctx: SparkSqlParser.MultipartIdentifierContext): Pair<String?, String> {
        return if (ctx.parts.size == 2) {
            return Pair(ctx.parts.get(0).text, ctx.parts.get(1).text)
        } else if (ctx.parts.size == 1) {
            Pair(null, ctx.parts.get(0).text)
        } else {
            throw SQLParserException("parse multipart error: " + ctx.parts.size)
        }
    }

    fun parseTableName(ctx: SparkSqlParser.MultipartIdentifierContext): TableId {
        return if (ctx.parts.size == 4) {
            TableId(ctx.parts.get(0).text, ctx.parts.get(1).text, ctx.parts.get(2).text, ctx.parts.get(3).text)
        } else if (ctx.parts.size == 3) {
            TableId(ctx.parts.get(0).text, ctx.parts.get(1).text, ctx.parts.get(2).text)
        } else if (ctx.parts.size == 2) {
            TableId(null, ctx.parts.get(0).text, ctx.parts.get(1).text)
        } else if (ctx.parts.size == 1) {
            TableId(null, null, ctx.parts.get(0).text)
        } else {
            throw SQLParserException("parse multipart error: " + ctx.parts.size)
        }
    }

    fun parseIdentifier(ctx: List<IdentifierContext>): String {
        return ctx.map { iden -> iden.text }.joinToString(",")
    }

    //-----------------------------------database-------------------------------------------------

    override fun visitCreateNamespace(ctx: SparkSqlParser.CreateNamespaceContext): Statement {
        var location: String = ""
        if (ctx.locationSpec().size > 0) {
            location = ctx.locationSpec().get(0).stringLit().text
            location = StringUtil.cleanQuote(location)
        }
        val type = ctx.namespace().text.uppercase()

        if (StringUtils.equalsIgnoreCase("database", type) ||
            StringUtils.equalsIgnoreCase("schema", type)) {

            val (catalogName, databaseName) = parseNamespace(ctx.multipartIdentifier())
            return CreateDatabase(catalogName, databaseName, location)
        } else {
            throw RuntimeException("not support: " + type)
        }
    }

    override fun visitDropNamespace(ctx: SparkSqlParser.DropNamespaceContext): Statement {
        val type = ctx.namespace().text.uppercase()
        if (StringUtils.equalsIgnoreCase("database", type) ||
            StringUtils.equalsIgnoreCase("schema", type)) {

            val (catalogName, databaseName) = parseNamespace(ctx.multipartIdentifier())
            return DropDatabase(catalogName, databaseName)
        } else {
            throw RuntimeException("not support: " + type)
        }
    }

    //-----------------------------------table-------------------------------------------------
    override fun visitCreateTable(ctx: SparkSqlParser.CreateTableContext): Statement {
        val tableId = parseTableName(ctx.createTableHeader().multipartIdentifier())
        return createTable(tableId,
            false,
            ctx.createTableHeader().TEMPORARY() != null,
            ctx.createTableHeader().EXTERNAL() != null,
            ctx.createTableHeader().IF() != null,
            ctx.createOrReplaceTableColTypeList(),
            ctx.createTableClauses(),
            ctx.tableProvider(),
            ctx.query())
    }


    override fun visitReplaceTable(ctx: SparkSqlParser.ReplaceTableContext): Statement {
        val tableId = parseTableName(ctx.replaceTableHeader().multipartIdentifier())
        return createTable(tableId,
            true,
            false,
            false,
            false,
            ctx.createOrReplaceTableColTypeList(),
            ctx.createTableClauses(),
            ctx.tableProvider(),
            ctx.query())
    }

    private fun createTable(
        tableId: TableId,
        replace: Boolean,
        temporary: Boolean,
        external: Boolean,
        ifNotExists: Boolean,
        createOrReplaceTableColTypeList: CreateOrReplaceTableColTypeListContext?,
        createTableClauses: CreateTableClausesContext,
        tableProvider: TableProviderContext?,
        query: QueryContext?): Statement {

        val comment = if (createTableClauses.commentSpec().size > 0) StringUtil.cleanQuote(createTableClauses.commentSpec(0).text) else null
        val lifeCycle = createTableClauses.lifecycle?.text?.toInt()

        var partitionColumnRels: List<ColumnRel>? = null
        val partitionColumnNames: ArrayList<String> = arrayListOf()
        var columnRels: List<ColumnRel>? = null
        var createTableType = "hive"
        if (query == null) {
            columnRels = createOrReplaceTableColTypeList?.createOrReplaceTableColType()?.map {
                val colName = it.colName.text
                val dataType = it.dataType().text
                val (nullable, defaultExpr, colComment) = parseColDefinition(it.colDefinitionOption())
                ColumnRel(colName, dataType, colComment, nullable, defaultExpr)
            }

            if (tableProvider != null) {
                createTableType = "spark"
            }

            if (createTableClauses.partitioning != null) {
                if ("spark" == createTableType) {
                    createTableClauses.partitioning.children
                        .filter { it is SparkSqlParser.PartitionTransformContext }.forEach { item ->
                            val column = item as SparkSqlParser.PartitionTransformContext
                            partitionColumnNames.add(column.text)
                        }

                    if (partitionColumnNames.size == 0) {
                        throw SQLParserException("spark create table 语法创建表，创建分区字段语法错误，请参考文档");
                    }
                } else {
                    partitionColumnRels = createTableClauses.partitioning.children
                        .filter { it is SparkSqlParser.PartitionColumnContext }.map { item ->
                            val column = item as SparkSqlParser.PartitionColumnContext
                            val colName = column.colType().colName.text
                            val dataType = column.colType().dataType().text
                            checkPartitionDataType(dataType)

                            partitionColumnNames.add(colName)
                            val colComment = if (column.colType().commentSpec() != null) StringUtil.cleanQuote(column.colType().commentSpec().text) else null
                            ColumnRel(colName, dataType, colComment)
                        }
                }
            }
        } else {
            if (createTableClauses.partitioning != null) {
                createTableClauses.partitioning.children
                    .filter { it is SparkSqlParser.PartitionTransformContext }.forEach { item ->
                        val column = item as SparkSqlParser.PartitionTransformContext
                        partitionColumnNames.add(column.text)
                    }
            }
        }

        val properties = HashMap<String, String>()
        if (createTableClauses.tableProps != null) {
            createTableClauses.tableProps.children.filter { it is SparkSqlParser.PropertyContext }.map { item ->
                val property = item as SparkSqlParser.PropertyContext
                val key = StringUtil.cleanQuote(property.key.text)
                val value = StringUtil.cleanQuote(property.value.text)
                properties.put(key, value)
            }
        }

        var fileFormat = tableProvider?.multipartIdentifier()?.text
        createTableClauses.createFileFormat()
        if (createTableClauses.createFileFormat().size == 1) {
            fileFormat = createTableClauses.createFileFormat().get(0).fileFormat().text
        }

        if (query != null) {
            currentOptType = StatementType.CREATE_TABLE_AS_SELECT
            val createTable = CreateTableAsSelect(tableId, comment, lifeCycle, partitionColumnRels, columnRels, properties, fileFormat, ifNotExists)
            createTable.createTableType = createTableType;
            createTable.replace = replace

            var querySql = StringUtils.substring(command, query.start.startIndex)
            if (StringUtils.startsWith(querySql, "(") && StringUtils.endsWith(querySql, ")")) {
                querySql = StringUtils.substringBetween(querySql, "(", ")")
            }

            createTable.querySql = querySql
            super.visitQuery(query)
            createTable.inputTables.addAll(inputTables)
            createTable.functionNames.addAll(functionNames)
            createTable.partitionColumnNames.addAll(partitionColumnNames)
            return createTable
        } else {
            currentOptType = StatementType.CREATE_TABLE
            val createTable = CreateTable(tableId, comment, lifeCycle, partitionColumnRels, columnRels, properties, fileFormat, ifNotExists)
            createTable.createTableType = createTableType;
            createTable.replace = replace
            createTable.external = external
            createTable.temporary = temporary

            if (createTableClauses.locationSpec().size > 0) {
                createTable.location = createTableClauses.locationSpec().get(0).text
            }

            createTable.partitionColumnNames = partitionColumnNames
            return createTable
        }
    }

    override fun visitCreateTableLike(ctx: SparkSqlParser.CreateTableLikeContext): Statement {
        val newDatabaseName = ctx.target.db?.text
        val newTableName = ctx.target.table.text

        val oldDatabaseName = ctx.source.db?.text
        val oldTableName = ctx.source.table.text

        val createTableLike = CreateTableLike(TableId(oldDatabaseName, oldTableName),
            TableId(newDatabaseName, newTableName))

        createTableLike.ifNotExists = ctx.NOT() != null
        return createTableLike
    }

    override fun visitDropTable(ctx: SparkSqlParser.DropTableContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        val dropTable = DropTable(tableId)
        dropTable.ifExists = ctx.EXISTS() != null
        return dropTable
    }

    override fun visitDropView(ctx: SparkSqlParser.DropViewContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        return DropView(tableId, ctx.EXISTS() != null)
    }

    override fun visitTruncateTable(ctx: SparkSqlParser.TruncateTableContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        return TruncateTable(tableId)
    }

    override fun visitRepairTable(ctx: SparkSqlParser.RepairTableContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        return RepairTable(tableId)
    }

    override fun visitRenameTable(ctx: SparkSqlParser.RenameTableContext): Statement {
        val tableId = parseTableName(ctx.from)
        val newTableId = parseTableName(ctx.to)

        return if (ctx.VIEW() != null) {
            val action = AlterTableAction(newTableId)
            AlterTable(RENAME_TABLE, tableId, action, TableType.VIEW)
        } else {
            val action = AlterTableAction(newTableId)
            AlterTable(RENAME_TABLE, tableId, action)
        }
    }

    override fun visitSetTableProperties(ctx: SparkSqlParser.SetTablePropertiesContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val properties = parseOptions(ctx.propertyList())
        val action = AlterTableAction()
        action.properties = properties

        return if (ctx.VIEW() == null) {
            AlterTable(SET_TABLE_PROPERTIES, tableId, action, TableType.VIEW)
        } else {
            AlterTable(SET_TABLE_PROPERTIES, tableId, action)
        }
    }

    override fun visitAddTableColumns(ctx: SparkSqlParser.AddTableColumnsContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        val columns = ctx.columns.children
            .filter { it is SparkSqlParser.QualifiedColTypeWithPositionContext }.map { item ->
                val column = item as SparkSqlParser.QualifiedColTypeWithPositionContext
                val columnName = column.multipartIdentifier().text
                val dataType = column.dataType().text
                val comment = if (column.commentSpec() != null) StringUtil.cleanQuote(column.commentSpec().text) else null

                var position: String? = null
                var afterCol: String? = null
                if (column.colPosition() != null) {
                    if (column.colPosition().FIRST() != null) {
                        position = "first"
                    } else if (column.colPosition().AFTER() != null) {
                        position = "after"
                        afterCol = column.colPosition().afterCol.text
                    }
                }

                AlterColumnAction(columnName, dataType, comment, position, afterCol)
            }

        val alterTable = AlterTable(ADD_COLUMN, tableId)
        alterTable.addActions(columns)
        return alterTable
    }

    override fun visitHiveChangeColumn(ctx: SparkSqlParser.HiveChangeColumnContext): Statement {
        val tableId = parseTableName(ctx.table)

        val columnName = ctx.colName.parts.get(0).text
        val newColumnName = ctx.colType().colName.text
        val dataType = ctx.colType().dataType().text
        val commentNode = ctx.colType().commentSpec()?.stringLit()
        val comment = if (commentNode != null) StringUtil.cleanQuote(commentNode.text) else null

        val action = AlterColumnAction(columnName, dataType, comment)
        action.newColumName = newColumnName
        if (ctx.colPosition() != null) {
            if (ctx.colPosition().FIRST() != null) {
                action.position = "first"
            } else if (ctx.colPosition().AFTER() != null) {
                action.position = "after"
                action.afterCol = ctx.colPosition().afterCol.text
            }
        }

        return AlterTable(ALTER_COLUMN, tableId, action)
    }

    override fun visitRenameTableColumn(ctx: SparkSqlParser.RenameTableColumnContext): Statement {
        val tableId = parseTableName(ctx.table)

        val columnName = ctx.from.text
        val newColumnName = ctx.to.text

        val action = AlterColumnAction(columnName)
        action.newColumName = newColumnName
        return AlterTable(RENAME_COLUMN, tableId, action)
    }

    override fun visitAlterTableAlterColumn(ctx: SparkSqlParser.AlterTableAlterColumnContext): Statement {
        val tableId = parseTableName(ctx.table)

        val action = parseAlterColumnAction(ctx.alterColumnAction())
        action.columName = ctx.column.text
        return AlterTable(ALTER_COLUMN, tableId, action)
    }

    override fun visitTouchTable(ctx: SparkSqlParser.TouchTableContext): Statement {
        val tableId = parseTableName(ctx.table)
        val action = AlterTableAction()
        val alterTable = AlterTable(TOUCH_TABLE, tableId, action)
        action.partitionVals = if (ctx.partitionSpec() != null) parsePartitionSpec(ctx.partitionSpec()) else null
        return alterTable
    }

    override fun visitDropTableColumns(ctx: SparkSqlParser.DropTableColumnsContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        val columns = ctx.columns.multipartIdentifier().map { id -> id.text }
        val action = DropColumnAction(columns.joinToString("."))
        return AlterTable(DROP_COLUMN, tableId, action)
    }

    override fun visitSetTableLocation(ctx: SparkSqlParser.SetTableLocationContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val location = StringUtil.cleanQuote(ctx.locationSpec().stringLit().text)

        val action = AlterTableAction()
        action.location = location
        return AlterTable(SET_TABLE_LOCATION, tableId, action)
    }

    override fun visitMergeFile(ctx: SparkSqlParser.MergeFileContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        val partitionVals = parsePartitionSpec(ctx.partitionSpec())
        val properties = parseOptions(ctx.propertyList())
        return MergeFileData(tableId, properties, partitionVals)
    }

    override fun visitRefreshTable(ctx: SparkSqlParser.RefreshTableContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        return RefreshStatement(tableId)
    }

    override fun visitAnalyze(ctx: SparkSqlParser.AnalyzeContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        return AnalyzeTable(listOf(tableId))
    }

    override fun visitDatatunnelExpr(ctx: SparkSqlParser.DatatunnelExprContext): Statement {
        val srcType = StringUtil.cleanQuote(ctx.sourceName.text)
        val distType = StringUtil.cleanQuote(ctx.sinkName.text)

        currentOptType = StatementType.DATATUNNEL

        if (ctx.ctes() != null) {
            visitCtes(ctx.ctes())
        }

        val srcOptions = parseDtOptions(ctx.readOpts)

        var transformSql: String? = null
        if (ctx.transfromSql != null) {
            transformSql = StringUtil.cleanQuote(ctx.transfromSql.text)
        }

        val distOptions = parseDtOptions(ctx.writeOpts)

        val data = DataTunnelExpr(srcType, srcOptions, transformSql, distType, distOptions)
        data.inputTables.addAll(inputTables)
        data.functionNames.addAll(functionNames)
        return data
    }

    override fun visitDatatunnelHelp(ctx: SparkSqlParser.DatatunnelHelpContext): Statement {
        val type = if (ctx.SOURCE() != null) "source" else "sink";
        val value = StringUtil.cleanQuote(ctx.value.text)
        return DataTunnelHelp(type, value)
    }

    private fun parseDtOptions(ctx: DtPropertyListContext?): HashMap<String, Any> {
        val options = HashMap<String, Any>()
        if (ctx != null) {
            ctx.dtProperty().map { item ->
                val property = item as DtPropertyContext
                val key = StringUtil.cleanQuote(property.key.text)
                if (property.value.columnDef().size > 0) {
                    val list = arrayListOf<Any>()
                    property.value.columnDef().map { col ->
                        val map = HashMap<String, String>()
                        col.dtColProperty().map { pt ->
                            val colProperty = pt as DtColPropertyContext
                            val colKey = StringUtil.cleanQuote(colProperty.key.text)
                            val colValue = StringUtil.cleanQuote(colProperty.value.text)
                            map.put(colKey, colValue)
                        }
                        list.add(map)
                    }
                    options.put(key, list)
                } else if (property.value.dtPropertyValue().size > 0) {
                    val list = arrayListOf<Any>()
                    property.value.dtPropertyValue().map { col ->
                        val value = StringUtil.cleanQuote(col.text)
                        list.add(value)
                    }
                    options.put(key, list)
                } else {
                    val value = StringUtil.cleanQuote(property.value.text)
                    options.put(key, value)
                }
            }
        }

        return options
    }

    override fun visitCall(ctx: SparkSqlParser.CallContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val properties = HashMap<String, String>()
        ctx.callArgument().filter { it.ARROW1() != null }.forEach { item ->
            val key = StringUtil.cleanQuote(item.identifier().text)
            val value = StringUtil.cleanQuote(item.expression().text)
            properties.put(key.lowercase(), value)
        }

        return CallProcedure(tableId.catalogName, tableId.schemaName, tableId.tableName, properties)
    }

    override fun visitCallHelp(ctx: SparkSqlParser.CallHelpContext): Statement {
        var procedure: String? = null
        if (ctx.callHelpExpr() != null) {
            procedure = if (ctx.callHelpExpr().callArgument() != null) {
                StringUtil.cleanQuote(ctx.callHelpExpr().callArgument().expression().text)
            } else {
                ctx.callHelpExpr().identifier().text
            }
        }

        return CallHelp(procedure)
    }

    override fun visitSync(ctx: SparkSqlParser.SyncContext): Statement {
        val type = ctx.type.text.lowercase();
        var owner: String? = null
        if (ctx.principal != null) {
            owner = ctx.principal.text
        }

        return if ("schema" == type) {
            val target = parseNamespace(ctx.target)
            val source = parseNamespace(ctx.source)
            SyncSchemaExpr(target.first, target.second, source.first, source.second, owner);
        } else {
            val targetTableId = parseTableName(ctx.target)
            val sourceTableId = parseTableName(ctx.source)
            SyncTableExpr(targetTableId, sourceTableId, owner)
        }
    }

    //-----------------------------------partition-------------------------------------------------

    override fun visitAddTablePartition(ctx: SparkSqlParser.AddTablePartitionContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        val partitions = ctx.partitionSpecLocation().map { parsePartitionSpec(it.partitionSpec()) }

        val ifNotExists = ctx.NOT() != null

        val action = AddPartitionAction(ifNotExists, partitions)
        return AlterTable(ADD_PARTITION, tableId, action)
    }

    override fun visitDropTablePartitions(ctx: SparkSqlParser.DropTablePartitionsContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val partitions = ctx.partitionSpec().map { parsePartitionSpec(it) }
        val ifExists = ctx.EXISTS() != null

        val action = DropPartitionAction(ifExists, partitions)
        return AlterTable(DROP_PARTITION, tableId, action)
    }

    override fun visitRenameTablePartition(ctx: SparkSqlParser.RenameTablePartitionContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val fromPartition = parsePartitionSpec(ctx.from)
        val toPartition = parsePartitionSpec(ctx.to)
        val action = RenamePartitionAction(fromPartition, toPartition)
        return AlterTable(RENAME_PARTITION, tableId, action)
    }

    //-----------------------------------view-------------------------------------------------

    override fun visitCreateView(ctx: SparkSqlParser.CreateViewContext): Statement {
        var comment: String? = null
        if (ctx.commentSpec().size > 0) {
            comment = ctx.commentSpec().get(0).stringLit().text
            comment = StringUtil.cleanQuote(comment)
        }

        val tableId = parseTableName(ctx.multipartIdentifier())
        val ifNotExists = ctx.NOT() != null

        var querySql = ""
        ctx.children.filter { it is QueryContext }.forEach { it ->
            val query = it as QueryContext
            querySql = StringUtils.substring(command, query.start.startIndex)
        }

        currentOptType = StatementType.CREATE_VIEW
        this.visitQuery(ctx.query())

        val createView = CreateView(tableId, querySql, comment, ifNotExists)
        createView.inputTables.addAll(inputTables)
        createView.functionNames.addAll(functionNames)

        if (ctx.REPLACE() != null) {
            createView.replace = true
        }
        if (ctx.TEMPORARY() != null) {
            createView.temporary = true
        }
        if (ctx.GLOBAL() != null) {
            createView.global = true
        }
        return createView
    }

    override fun visitCreateTempViewUsing(ctx: SparkSqlParser.CreateTempViewUsingContext): Statement {
        val tableName = ctx.tableIdentifier().table.text
        var databaseName: String? = null
        if (ctx.tableIdentifier().db != null) {
            databaseName = ctx.tableIdentifier().db.text
        }

        currentOptType = StatementType.CREATE_TEMP_VIEW_USING

        val tableId = TableId(null, databaseName, tableName)
        val fileFormat = ctx.tableProvider().multipartIdentifier().text
        val properties = parseOptions(ctx.propertyList())
        val createView = CreateTempViewUsing(tableId, fileFormat, properties)
        if (ctx.REPLACE() != null) {
            createView.replace = true
        }
        if (ctx.GLOBAL() != null) {
            createView.global = true
        }
        return createView
    }

    override fun visitAlterViewQuery(ctx: SparkSqlParser.AlterViewQueryContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())

        var querySql = ""
        ctx.children.filter { it is QueryContext }.forEach {
            val query = it as QueryContext
            querySql = StringUtils.substring(command, query.start.startIndex)
        }

        currentAlterType = ALTER_VIEW
        visitQuery(ctx.query())

        val action = AlterViewAction(querySql, inputTables, functionNames)
        return AlterTable(ALTER_VIEW, tableId, action)
    }

    override fun visitCreateIndex(ctx: SparkSqlParser.CreateIndexContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val indexName = parseIdentifier(ctx.identifier())
        val createIndex = CreateIndex(indexName)
        return AlterTable(ADD_INDEX, tableId, createIndex)
    }

    override fun visitDropIndex(ctx: SparkSqlParser.DropIndexContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val indexName = ctx.identifier().text
        val dropIndex = DropIndex(indexName)
        return AlterTable(DROP_INDEX, tableId, dropIndex)
    }

    //-----------------------------------function-------------------------------------------------

    override fun visitCreateFunction(ctx: SparkSqlParser.CreateFunctionContext): Statement {
        val functionId = parseTableName(ctx.multipartIdentifier())
        val classNmae = ctx.className.text

        var temporary = false
        var file: String? = null
        if (ctx.TEMPORARY() != null) {
            temporary = true
        } else {
            file = ctx.resource(0).stringLit().text
        }

        val replace = if (ctx.REPLACE() != null) true else false
        return CreateFunction(FunctionId(functionId.schemaName, functionId.tableName), replace, temporary, classNmae, file)
    }

    override fun visitDropFunction(ctx: SparkSqlParser.DropFunctionContext): Statement {
        val functionId = parseTableName(ctx.multipartIdentifier())
        return DropFunction(functionId.schemaName, functionId.tableName)
    }
    //-----------------------------------cache-------------------------------------------------

    override fun visitCacheTable(ctx: SparkSqlParser.CacheTableContext?): Statement {
        return DefaultStatement(StatementType.CACHE)
    }

    override fun visitUncacheTable(ctx: SparkSqlParser.UncacheTableContext?): Statement {
        return DefaultStatement(StatementType.UNCACHE)
    }

    override fun visitClearCache(ctx: SparkSqlParser.ClearCacheContext?): Statement {
        return DefaultStatement(StatementType.CLEAR_CACHE)
    }

    //-----------------------------------other-------------------------------------------------

    override fun visitExplain(ctx: SparkSqlParser.ExplainContext?): Statement {
        return DefaultStatement(StatementType.EXPLAIN)
    }

    override fun visitCreateFileView(ctx: SparkSqlParser.CreateFileViewContext): Statement {
        val tableId = parseTableName(ctx.multipartIdentifier())
        val path = StringUtil.cleanQuote(ctx.path.text)

        var compression: String? = null
        var sizeLimit: String? = null

        val fileFormat = ctx.tableProvider().multipartIdentifier().text

        val causes = ctx.createFileViewClauses()
        if (causes != null) {
            if (causes.compressionName != null) compression = causes.compressionName.text
            if (causes.sizelimit != null) sizeLimit = causes.sizelimit.text
        }
        val properties = parseOptions(ctx.propertyList())

        return CreateFileView(tableId, path, properties, fileFormat, compression, sizeLimit)
    }

    override fun visitExportTable(ctx: SparkSqlParser.ExportTableContext): Statement {
        if (ctx.ctes() != null) {
            visitCtes(ctx.ctes())
        }
        currentOptType = StatementType.EXPORT_TABLE
        super.visitExportTable(ctx)

        val tableId = parseTableName(ctx.multipartIdentifier())
        val filePath = StringUtil.cleanQuote(ctx.filePath.text)
        val properties = parseOptions(ctx.propertyList())
        val partitionVals = parsePartitionSpec(ctx.partitionSpec())

        var fileFormat: String? = null
        var compression: String? = null
        var maxFileSize: String? = null
        var overwrite: Boolean = false
        var single: Boolean = false

        val causes = ctx.exportTableClauses()
        if (causes != null) {
            if (causes.fileformatName != null) fileFormat = causes.fileformatName.text
            if (causes.compressionName != null) compression = causes.compressionName.text
            if (causes.maxfilesize != null) maxFileSize = causes.maxfilesize.text
            if (causes.overwrite != null) overwrite = causes.overwrite.TRUE() != null
            if (causes.single != null) single = causes.single.TRUE() != null
        }

        val exportData = ExportData(tableId, filePath, properties, partitionVals,
            fileFormat, compression, maxFileSize, overwrite, single, inputTables)

        exportData.functionNames.addAll(functionNames)
        return exportData
    }

    override fun visitUse(ctx: SparkSqlParser.UseContext): Statement {
        val (catalogName, databaseName) = parseNamespace(ctx.multipartIdentifier())
        return UseDatabase(catalogName, databaseName)
    }

    override fun visitUseNamespace(ctx: SparkSqlParser.UseNamespaceContext): Statement {
        val type = ctx.namespace().text.uppercase()

        if (StringUtils.equalsIgnoreCase("database", type) ||
            StringUtils.equalsIgnoreCase("schema", type)) {

            val (catalogName, databaseName) = parseNamespace(ctx.multipartIdentifier())
            return UseDatabase(catalogName, databaseName)
        } else if (StringUtils.equalsIgnoreCase("namespace", type)) {
            return UseCatalog(ctx.multipartIdentifier().text)
        } else {
            throw RuntimeException("not support: " + type)
        }
    }

    override fun visitSetConfiguration(ctx: SparkSqlParser.SetConfigurationContext?): Statement {
        return DefaultStatement(StatementType.SET)
    }

    //-----------------------------------insert & query-------------------------------------------------

    override fun visitStatementDefault(ctx: SparkSqlParser.StatementDefaultContext): Statement? {
        val node = ctx.getChild(0)
        if (node is QueryContext) {
            currentOptType = StatementType.SELECT
            super.visitStatementDefault(ctx)

            val queryStmt = QueryStmt(inputTables, limit, offset)
            queryStmt.functionNames.addAll(functionNames)
            return queryStmt
        }

        return null
    }

    override fun visitDmlStatement(ctx: SparkSqlParser.DmlStatementContext): Statement? {
        currentOptType = StatementType.INSERT
        val node = if (ctx.ctes() != null) {
            this.visitCtes(ctx.ctes())
            ctx.getChild(1)
        } else {
            ctx.getChild(0)
        }

        if (node is SingleInsertQueryContext) {
            currentOptType = StatementType.INSERT
            insertSql = true
            super.visitQuery(node.query())
            val singleInsertStmt = parseInsertInto(node.insertInto())

            val querySql = StringUtils.substring(command, node.query().start.startIndex)
            singleInsertStmt.querySql = querySql
            singleInsertStmt.inputTables.addAll(inputTables)
            singleInsertStmt.functionNames.addAll(functionNames)
            singleInsertStmt.rows = rows

            return singleInsertStmt
        } else if (StringUtils.equalsIgnoreCase("from", ctx.start.text)) {
            currentOptType = StatementType.INSERT
            super.visitDmlStatement(ctx)

            val insertTable = InsertTable(InsertMode.OVERWRITE, outputTables.first())
            insertTable.inputTables.addAll(inputTables)
            insertTable.outputTables.addAll(outputTables)
            insertTable.functionNames.addAll(functionNames)
            return insertTable
        } else if (node is SparkSqlParser.UpdateTableContext ||
                node is SparkSqlParser.DeleteFromTableContext ||
                node is SparkSqlParser.MergeIntoTableContext) {
            return super.visitDmlStatement(ctx)
        } else {
            return null
        }
    }

    private fun parseInsertInto(ctx: InsertIntoContext): InsertTable {
        return if (ctx is SparkSqlParser.InsertIntoTableContext) {
            val tableId = parseTableName(ctx.multipartIdentifier())
            val partitionVals = parsePartitionSpec(ctx.partitionSpec())
            val stmt = InsertTable(InsertMode.INTO, tableId)
            stmt.partitionVals = partitionVals
            stmt
        } else if (ctx is SparkSqlParser.InsertOverwriteTableContext) {
            val tableId = parseTableName(ctx.multipartIdentifier())
            val partitionVals = parsePartitionSpec(ctx.partitionSpec())
            val stmt = InsertTable(InsertMode.OVERWRITE, tableId)
            stmt.partitionVals = partitionVals
            stmt
        } else if (ctx is SparkSqlParser.InsertIntoReplaceWhereContext) {
            val tableId = parseTableName(ctx.multipartIdentifier())
            InsertTable(InsertMode.INTO_REPLACE, tableId)
        } else if (ctx is SparkSqlParser.InsertOverwriteDirContext) {
            val path = if (ctx.path != null) ctx.path.STRING().text else "";
            val properties = parseOptions(ctx.propertyList())
            val fileFormat = ctx.tableProvider().multipartIdentifier().text

            val stmt = InsertTable(InsertMode.OVERWRITE_DIR, TableId(path))
            stmt.properties = properties
            stmt.fileFormat = fileFormat
            stmt
        } else {
            throw SQLParserException("not support InsertMode.OVERWRITE_HIVE_DIR")
        }
    }

    //-----------------------------------delta sql-------------------------------------------------

    override fun visitDeleteFromTable(ctx: SparkSqlParser.DeleteFromTableContext): Statement {
        currentOptType = StatementType.DELETE
        val tableId = parseTableName(ctx.multipartIdentifier())
        super.visitWhereClause(ctx.whereClause())

        return DeleteTable(tableId, inputTables)
    }

    override fun visitUpdateTable(ctx: SparkSqlParser.UpdateTableContext): Statement {
        currentOptType = StatementType.UPDATE
        val tableId = parseTableName(ctx.multipartIdentifier())
        if (ctx.whereClause() != null) {
            super.visitWhereClause(ctx.whereClause())
        }

        return UpdateTable(tableId, inputTables)
    }

    override fun visitMergeIntoTable(ctx: SparkSqlParser.MergeIntoTableContext): Statement {
        currentOptType = StatementType.MERGE

        val targetTable = parseTableName(ctx.target)
        val mergeTable = MergeTable(targetTable = targetTable)

        if (ctx.source != null) {
            val tableId = parseTableName(ctx.source)
            inputTables.add(tableId)
        } else if (ctx.sourceQuery != null && ctx.sourceQuery is QueryContext) {
            val query = ctx.sourceQuery as QueryContext
            super.visitQuery(query)
        }
        mergeTable.inputTables = inputTables
        return mergeTable
    }

    //-----------------------------------private method-------------------------------------------------

    override fun visitFunctionName(ctx: SparkSqlParser.FunctionNameContext): Statement? {
        if (StatementType.SELECT == currentOptType ||
            StatementType.CREATE_VIEW == currentOptType ||
            StatementType.INSERT == currentOptType ||
            StatementType.CREATE_TABLE_AS_SELECT == currentOptType ||
            StatementType.MERGE == currentOptType ||
            StatementType.EXPORT_TABLE == currentOptType ||
            StatementType.DATATUNNEL == currentOptType) {

            functionNames.add(StringUtils.lowerCase(ctx.qualifiedName().text))
        }
        return super.visitFunctionName(ctx)
    }

    override fun visitCtes(ctx: SparkSqlParser.CtesContext): Statement? {
        ctx.namedQuery().forEach {
            cteTempTables.add(TableId(it.name.text))
        }
        return super.visitCtes(ctx)
    }

    override fun visitMultipartIdentifier(ctx: SparkSqlParser.MultipartIdentifierContext): Statement? {
        val tableId = parseTableName(ctx)
        if (currentOptType == StatementType.CREATE_TABLE_AS_SELECT ||
            currentOptType == StatementType.SELECT ||
            currentOptType == StatementType.CREATE_VIEW ||
            currentOptType == StatementType.INSERT ||
            currentOptType == StatementType.MERGE ||
            currentOptType == StatementType.EXPORT_TABLE ||
            currentOptType == StatementType.DATATUNNEL ||
            currentOptType == StatementType.UPDATE ||
            currentOptType == StatementType.DELETE ||
            currentAlterType == ALTER_VIEW) {

            if (!inputTables.contains(tableId) && !cteTempTables.contains(tableId)) {
                inputTables.add(tableId)
            }
        }
        return null
    }

    /*override fun visitRowConstructor(ctx: SparkSqlParser.RowConstructorContext): Statement? {
        val row = ctx.children.filter { it is SparkSqlParser.NamedExpressionContext }.map {
            var text = it.text
            text = StringUtil.cleanQuote(text)
            text
        }.toList()

        rows.add(row)
        return super.visitRowConstructor(ctx)
    }*/

    override fun visitInlineTable(ctx: SparkSqlParser.InlineTableContext): Statement? {
        ctx.children.filter { it is SparkSqlParser.ExpressionContext }.forEach {
            var text = it.text
            text = StringUtils.substringBetween(text, "(", ")").trim()
            text = StringUtil.cleanQuote(text)
            val list = listOf(text)
            rows.add(list)
        }
        return super.visitInlineTable(ctx)
    }

    override fun visitFromClause(ctx: SparkSqlParser.FromClauseContext): Statement? {
        multiInsertToken = "from"
        return super.visitFromClause(ctx)
    }

    override fun visitMultiInsertQueryBody(ctx: SparkSqlParser.MultiInsertQueryBodyContext): Statement? {
        multiInsertToken = "insert"
        val obj = ctx.insertInto()
        if (obj is SparkSqlParser.InsertOverwriteTableContext) {
            val multipartIdentifier = obj.multipartIdentifier()
            val tableId = parseTableName(multipartIdentifier)
            outputTables.add(tableId)
        } else if (obj is SparkSqlParser.InsertIntoTableContext) {
            val multipartIdentifier = obj.multipartIdentifier()
            val tableId = parseTableName(multipartIdentifier)
            outputTables.add(tableId)
        }
        return super.visitMultiInsertQueryBody(ctx)
    }

    override fun visitQueryOrganization(ctx: SparkSqlParser.QueryOrganizationContext): Statement? {
        limit = ctx.limit?.text?.toInt()
        offset = ctx.offset?.text?.toInt()
        return super.visitQueryOrganization(ctx)
    }

    override fun visitTypeConstructor(ctx: SparkSqlParser.TypeConstructorContext): Statement? {
        val valueType = ctx.identifier().getText().uppercase()
        if (!("DATE".equals(valueType) || "TIME".equals(valueType)
                    || "TIMESTAMP".equals(valueType) || "INTERVAL".equals(valueType)
                    || "X".equals(valueType))) {
            throw SQLParserException("Literals of type " + valueType + " are currently not supported.");
        }

        return super.visitTypeConstructor(ctx)
    }

    private fun parseColDefinition(colDef: List<ColDefinitionOptionContext>): Triple<Boolean, String?, String?> {
        var nullable: Boolean = false
        var comment: String? = null
        var defaultExpr: String? = null

        if (colDef.size > 0) {
            colDef.forEach { col ->
                if (col.NULL() != null) {
                    nullable = true
                }

                if (col.commentSpec() != null) {
                    comment = StringUtil.cleanQuote(col.commentSpec().stringLit().text);
                }

                if (col.defaultExpression() != null) {
                    defaultExpr = StringUtils.substring(command,
                        col.defaultExpression().start.startIndex,
                        col.defaultExpression().stop.stopIndex + 1)

                    if (defaultExpr != null) {
                        defaultExpr = StringUtil.cleanQuote(defaultExpr!!)
                    }
                }
            }
        }

        return Triple(nullable, defaultExpr, comment)
    }

    private fun parseAlterColumnAction(context: AlterColumnActionContext): AlterColumnAction {
        val action = AlterColumnAction();
        if (context.dataType() != null) {
            action.dataType = StringUtils.substring(command, context.dataType().start.startIndex,
                context.dataType().stop.stopIndex + 1)
        }

        if (context.commentSpec() != null) {
            action.comment = StringUtil.cleanQuote(context.commentSpec().stringLit().text)
        }

        if (context.colPosition() != null) {
            if (context.colPosition().FIRST() != null) {
                action.position = "first"
            } else if (context.colPosition().AFTER() != null) {
                action.position = "after"
                action.afterCol = context.colPosition().afterCol.text
            }
        }

        if (context.setOrDrop != null) {
            if (StringUtils.containsAnyIgnoreCase(context.setOrDrop.text, "drop")) {
                action.setOrDrop = "DROP";
            } else {
                action.setOrDrop = "SET";
            }

            if (context.NOT() != null) {
                action.nullable = false
            }
        }

        if (context.defaultExpression() != null) {
            val expr = context.defaultExpression().expression()
            action.defaultExpression = StringUtils.substring(command, expr.start.startIndex,
                expr.stop.stopIndex + 1)
        }

        if (context.dropDefault != null) {
            action.dropDefault = true;
        }

        return action
    }

    /**
     * 表列支持数据类型
     */
    private fun checkColumnDataType(dataType: String): Boolean {
        if (StringUtils.startsWithIgnoreCase(dataType, "decimal")) {
            return true
        }

        return when (dataType.lowercase()) {
            "string", "int", "bigint", "double", "date", "timestamp", "boolean" -> true
            else -> throw IllegalStateException("不支持数据类型：" + dataType)
        }
    }

    /**
     * 分区支持数据类型
     */
    private fun checkPartitionDataType(dataType: String): Boolean {
        return when (dataType.lowercase()) {
            "string", "int", "bigint" -> true
            else -> throw IllegalStateException("不支持数据类型：" + dataType)
        }
    }

    private fun parseOptions(ctx: PropertyListContext?): Map<String, String> {
        val properties = HashMap<String, String>()
        if (ctx != null) {
            ctx.property().forEach { item ->
                val property = item as SparkSqlParser.PropertyContext
                val key = StringUtil.cleanQuote(property.key.text)
                val value = StringUtil.cleanQuote(property.value.text)
                properties.put(key, value)
            }
        }

        return properties
    }

    private fun parsePartitionSpec(ctx: PartitionSpecContext?): LinkedHashMap<String, String> {
        val partitions: LinkedHashMap<String, String> = LinkedHashMap()
        if (ctx != null) {
            val count = ctx.partitionVal().size
            ctx.partitionVal().forEach {
                if (count == 1) {
                    partitions.put(it.identifier().text, "__dynamic__")
                } else {
                    var value = it.getChild(2).text
                    value = StringUtil.cleanQuote(value)
                    partitions.put(it.identifier().text, value)
                }
            }
        }
        return partitions
    }
}
