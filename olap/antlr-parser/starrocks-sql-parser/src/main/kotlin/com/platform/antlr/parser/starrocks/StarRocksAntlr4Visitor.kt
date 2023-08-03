package com.platform.antlr.parser.starrocks

import com.platform.antlr.parser.common.util.StringUtil
import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.relational.*
import com.platform.antlr.parser.common.relational.create.*
import com.platform.antlr.parser.common.relational.table.ColumnRel
import com.platform.antlr.parser.common.relational.dml.*
import com.platform.antlr.parser.common.relational.drop.*
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.starrocks.antlr4.StarRocksParserBaseVisitor
import org.antlr.v4.runtime.tree.RuleNode

import com.platform.antlr.parser.starrocks.antlr4.StarRocksParser.*
import org.apache.commons.lang3.StringUtils

/**
 * Created by libinsong on 2020/6/30 9:59 上午
 */
class StarRocksAntlr4Visitor: StarRocksParserBaseVisitor<Statement>() {

    private var command: String? = null

    private var currentOptType: StatementType = StatementType.UNKOWN
    private var limit: Int? = null
    private var offset: Int? = null

    private var queryStmts: ArrayList<QueryStmt> = arrayListOf()
    private var inputTables: ArrayList<TableId> = arrayListOf()
    private var outputTables: ArrayList<TableId> = arrayListOf()
    private var cteTempTables: ArrayList<TableId> = arrayListOf()

    fun setCommand(command: String) {
        this.command = command
    }

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Statement?): Boolean {
        return if (currentResult == null) true else false
    }

    override fun visitCreateExternalCatalogStatement(ctx: CreateExternalCatalogStatementContext): Statement {
        val catalogName: String = StringUtil.cleanQuote(ctx.catalogName.text)
        val properties = parseOptions(ctx.properties())
        return CreateCatalog(catalogName, properties)
    }

    override fun visitDropExternalCatalogStatement(ctx: DropExternalCatalogStatementContext): Statement {
        val catalogName: String = StringUtil.cleanQuote(ctx.catalogName.text)
        return DropCatalog(catalogName)
    }

    override fun visitCreateDbStatement(ctx: CreateDbStatementContext): Statement {
        val catalogName: String? = if (ctx.catalog != null) StringUtil.cleanQuote(ctx.catalog.text) else null
        val databaseName: String = StringUtil.cleanQuote(ctx.database.text)
        val properties = parseOptions(ctx.properties())
        val ifNotExists = ctx.NOT() != null

        return CreateDatabase(catalogName, databaseName, properties, ifNotExists)
    }

    override fun visitDropDbStatement(ctx: DropDbStatementContext): Statement {
        val catalogName: String? = if (ctx.catalog != null) StringUtil.cleanQuote(ctx.catalog.text) else null
        val databaseName: String = StringUtil.cleanQuote(ctx.database.text)
        val ifExists = ctx.EXISTS() != null
        return DropDatabase(catalogName, databaseName, ifExists)
    }

    override fun visitCreateTableStatement(ctx: CreateTableStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val comment = if (ctx.comment() != null) StringUtil.cleanQuote(ctx.comment().text) else null
        val columnRels: List<ColumnRel> = ctx.columnDesc().map { column ->
            val columnName = column.identifier().text
            val dataType = column.type().text
            val colComment = if (column.comment() != null) StringUtil.cleanQuote(column.comment().string().text) else null
            ColumnRel(columnName, dataType, colComment)
        }

        return CreateTable(tableId, comment, columnRels)
    }

    override fun visitCreateViewStatement(ctx: CreateViewStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val comment: String? = if (ctx.comment() != null) StringUtil.cleanQuote(ctx.comment().string().text) else null
        val columns = parseColumnNameWithComment(ctx.columnNameWithComment());
        val ifNotExists = ctx.NOT() != null
        val querySql = StringUtils.substring(command, ctx.queryStatement().start.startIndex)
        val createView = CreateView(tableId, querySql, comment, ifNotExists, columns)

        this.visitQueryStatement(ctx.queryStatement())
        createView.inputTables.addAll(inputTables)
        return createView;
    }

    override fun visitCreateMaterializedViewStatement(ctx: CreateMaterializedViewStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val comment: String? = if (ctx.comment() != null) StringUtil.cleanQuote(ctx.comment().string().text) else null
        val columns = parseColumnNameWithComment(ctx.columnNameWithComment());
        val ifNotExists = ctx.NOT() != null
        val querySql = StringUtils.substring(command, ctx.queryStatement().start.startIndex)
        val createView = CreateMaterializedView(tableId, querySql, comment, ifNotExists, columns)

        this.visitQueryStatement(ctx.queryStatement())
        createView.inputTables.addAll(inputTables)
        return createView;
    }

    override fun visitDropTableStatement(ctx: DropTableStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val ifExists = ctx.EXISTS() != null
        val dropTable = DropTable(tableId, ifExists)
        dropTable.force = ctx.FORCE() != null
        return dropTable
    }

    override fun visitDropViewStatement(ctx: DropViewStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val ifExists = ctx.EXISTS() != null
        return DropView(tableId, ifExists)
    }

    override fun visitDropMaterializedViewStatement(ctx: DropMaterializedViewStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val ifExists = ctx.EXISTS() != null
        return DropMaterializedView(tableId, ifExists)
    }

    override fun visitCreateIndexStatement(ctx: CreateIndexStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val indexName = ctx.indexName.text
        val comment = if (ctx.comment() != null) StringUtil.cleanQuote(ctx.comment().text) else null

        val columns = ctx.identifierList().identifier().map { identifier ->
            IndexColumnName(identifier.text)
        }

        val createIndex = CreateIndex(indexName)
        createIndex.comment = comment
        createIndex.indexColumnNames.addAll(columns)
        return AlterTable(AlterType.ADD_INDEX, tableId, createIndex)
    }

    override fun visitDropIndexStatement(ctx: DropIndexStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        val indexName = ctx.indexName.text
        val dropIndex = DropIndex(indexName)
        return AlterTable(AlterType.DROP_INDEX, tableId, dropIndex)
    }

    override fun visitAlterTableStatement(ctx: AlterTableStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        return AlterTable(AlterType.UNKOWN, tableId)
    }

    override fun visitAlterViewStatement(ctx: AlterViewStatementContext): Statement {
        val tableId = parseTableName(ctx.qualifiedName())
        return AlterTable(AlterType.ALTER_VIEW, tableId)
    }

    override fun visitQueryStatement(ctx: QueryStatementContext): Statement {
        currentOptType = StatementType.SELECT
        super.visitQueryRelation(ctx.queryRelation())
        val queryStmt = QueryStmt(inputTables, limit, offset)

        queryStmts.add(queryStmt)
        return queryStmt
    }

    override fun visitDeleteStatement(ctx: DeleteStatementContext): Statement {
        currentOptType = StatementType.DELETE
        val tableId = parseTableName(ctx.qualifiedName())
        if (ctx.withClause() != null) {
            visitWithClause(ctx.withClause())
        }
        visit(ctx.where)
        if (ctx.relations() != null) {
            visit(ctx.relations())
        }

        return DeleteTable(tableId, inputTables)
    }

    override fun visitUpdateStatement(ctx: UpdateStatementContext): Statement {
        currentOptType = StatementType.UPDATE
        val tableId = parseTableName(ctx.qualifiedName())
        if (ctx.withClause() != null) {
            visitWithClause(ctx.withClause())
        }
        visit(ctx.where)
        visit(ctx.fromClause())

        return UpdateTable(tableId, inputTables)
    }

    override fun visitInsertStatement(ctx: InsertStatementContext): Statement {
        currentOptType = StatementType.INSERT
        val tableId = parseTableName(ctx.qualifiedName())
        visitQueryStatement(ctx.queryStatement())

        val insertTable =
            if (ctx.INTO() != null) InsertTable(InsertMode.INTO, tableId)
            else InsertTable(InsertMode.OVERWRITE, tableId)

        insertTable.inputTables.addAll(inputTables)
        return insertTable
    }

    override fun visitQualifiedName(ctx: QualifiedNameContext): Statement? {
        if (currentOptType == StatementType.SELECT ||
            currentOptType == StatementType.CREATE_VIEW ||
            currentOptType == StatementType.CREATE_MATERIALIZED_VIEW ||
            currentOptType == StatementType.UPDATE ||
            currentOptType == StatementType.DELETE ||
            currentOptType == StatementType.MERGE ||
            currentOptType == StatementType.INSERT ||
            currentOptType == StatementType.CREATE_FUNCTION) {

            val tableId = parseTableName(ctx)
            if (!inputTables.contains(tableId) && !cteTempTables.contains(tableId)) {
                inputTables.add(tableId)
            }
        }
        return null
    }

    override fun visitWithClause(ctx: WithClauseContext): Statement? {
        ctx.commonTableExpression().forEach {
            cteTempTables.add(TableId(it.name.text))
        }
        return super.visitWithClause(ctx)
    }

    override fun visitLimitElement(ctx: LimitElementContext): Statement? {
        if (ctx.limit != null) {
            limit = ctx.limit.text.toInt()
        }
        if (ctx.offset != null) {
            offset = ctx.offset.text.toInt()
        }

        return super.visitLimitElement(ctx)
    }

    fun parseTableName(ctx: QualifiedNameContext): TableId {
        return if (ctx.identifier().size == 3) {
            val catalotName = ctx.identifier().get(0).text
            val schemaName = ctx.identifier().get(1).text
            val tableName = ctx.identifier().get(2).text
            TableId(catalotName, schemaName, tableName)
        } else if (ctx.identifier().size == 2) {
            val schemaName = ctx.identifier().get(0).text
            val tableName = ctx.identifier().get(1).text
            TableId(schemaName, tableName)
        } else if (ctx.identifier().size == 1) {
            val tableName = ctx.identifier().get(0).text
            TableId(tableName)
        } else {
            throw SQLParserException("parse qualifiedName error: " + ctx.identifier().size)
        }
    }

    private fun parseOptions(ctx: PropertiesContext?): Map<String, String> {
        val properties = HashMap<String, String>()
        if (ctx != null) {
            ctx.property().forEach { item ->
                val property = item as PropertyContext
                val key = StringUtil.cleanQuote(property.key.text)
                val value = StringUtil.cleanQuote(property.value.text)
                properties.put(key, value)
            }
        }

        return properties
    }

    private fun parseColumnNameWithComment(columns: List<ColumnNameWithCommentContext>): List<ColumnRel> {
        return columns.map { col ->
            val name = col.columnName.text
            val comment = StringUtil.cleanQuote(col.comment().string().text)
            ColumnRel(name, null, comment)
        }
    }
}
