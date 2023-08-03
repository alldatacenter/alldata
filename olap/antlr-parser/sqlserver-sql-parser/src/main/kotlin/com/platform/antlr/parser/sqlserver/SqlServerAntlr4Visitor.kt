package com.platform.antlr.parser.sqlserver

import com.platform.antlr.parser.common.SQLParserException
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.dml.*
import com.platform.antlr.parser.sqlserver.antlr4.SqlServerParser
import com.platform.antlr.parser.sqlserver.antlr4.SqlServerParserBaseVisitor
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode

/**
 * Created by libinsong on 2020/6/30 9:59 上午
 */
class SqlServerAntlr4Visitor: SqlServerParserBaseVisitor<Statement>() {

    private var currentOptType: StatementType = StatementType.UNKOWN

    private var limit: Int? = null
    private var offset: Int? = null
    private var inputTables: ArrayList<TableId> = arrayListOf()
    private var outputTables: ArrayList<TableId> = arrayListOf()
    private var cteTempTables: ArrayList<TableId> = arrayListOf()

    override fun visit(tree: ParseTree?): Statement {
        val data = super.visit(tree)

        if (data == null) {
            throw SQLParserException("不支持的SQL")
        }

        return data;
    }

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Statement?): Boolean {
        return if (currentResult == null) true else false
    }

    override fun visitSelect_statement_standalone(ctx: SqlServerParser.Select_statement_standaloneContext): Statement {
        currentOptType = StatementType.SELECT
        if (ctx.with_expression() != null) {
            this.visitWith_expression(ctx.with_expression())
        }
        super.visitSelect_statement(ctx.select_statement())

        val queryStmt = QueryStmt(inputTables, limit, offset)
        queryStmt.inputTables = inputTables
        return queryStmt
    }

    override fun visitDelete_statement(ctx: SqlServerParser.Delete_statementContext): Statement {
        currentOptType = StatementType.DELETE
        val tableId = parseTableName(ctx.delete_statement_from().ddl_object().full_table_name())
        if (ctx.with_expression() != null) {
            this.visitWith_expression(ctx.with_expression())
        }
        if (ctx.search_condition() != null) {
            super.visitSearch_condition(ctx.search_condition())
        }

        return DeleteTable(tableId, inputTables)
    }

    override fun visitInsert_statement(ctx: SqlServerParser.Insert_statementContext): Statement {
        currentOptType = StatementType.INSERT
        val tableId = parseTableName(ctx.ddl_object().full_table_name())
        if (ctx.with_expression() != null) {
            this.visitWith_expression(ctx.with_expression())
        }

        val insertTable =
            if (ctx.INTO() != null) InsertTable(InsertMode.INTO, tableId)
            else InsertTable(InsertMode.OVERWRITE, tableId)

        insertTable.inputTables.addAll(inputTables)
        return insertTable
    }

    override fun visitUpdate_statement(ctx: SqlServerParser.Update_statementContext): Statement {
        currentOptType = StatementType.UPDATE
        val tableId = parseTableName(ctx.ddl_object().full_table_name())
        if (ctx.with_expression() != null) {
            this.visitWith_expression(ctx.with_expression())
        }

        if (ctx.table_sources() != null) {
            super.visitTable_sources(ctx.table_sources())
        }

        if (ctx.search_condition() != null) {
            super.visitSearch_condition(ctx.search_condition())
        }

        return UpdateTable(tableId, inputTables)
    }

    override fun visitMerge_statement(ctx: SqlServerParser.Merge_statementContext): Statement {
        currentOptType = StatementType.MERGE

        val tableId = parseTableName(ctx.ddl_object().full_table_name())
        val mergeTable = MergeTable(tableId)
        if (ctx.with_expression() != null) {
            this.visitWith_expression(ctx.with_expression())
        }

        if (ctx.table_sources() != null) {
            super.visitTable_sources(ctx.table_sources())
        }

        mergeTable.inputTables = inputTables
        return mergeTable
    }

    override fun visitWith_expression(ctx: SqlServerParser.With_expressionContext): Statement? {
        ctx.common_table_expression().forEach {
            cteTempTables.add(TableId(it.id_().text))
        }
        return super.visitWith_expression(ctx)
    }

    override fun visitSelect_order_by_clause(ctx: SqlServerParser.Select_order_by_clauseContext): Statement? {
        if (ctx.OFFSET() != null) {
            offset = ctx.offset_exp.text.toInt()
        }

        if (ctx.OFFSET() != null) {
            limit = ctx.fetch_exp.text.toInt()
        }

        return super.visitSelect_order_by_clause(ctx)
    }

    override fun visitTable_source_item(ctx: SqlServerParser.Table_source_itemContext): Statement? {
        if (currentOptType == StatementType.SELECT ||
            currentOptType == StatementType.CREATE_VIEW ||
            currentOptType == StatementType.CREATE_MATERIALIZED_VIEW ||
            currentOptType == StatementType.UPDATE ||
            currentOptType == StatementType.DELETE ||
            currentOptType == StatementType.MERGE ||
            currentOptType == StatementType.INSERT ||
            currentOptType == StatementType.CREATE_FUNCTION) {

            if (ctx.full_table_name() != null) {
                val tableId = parseTableName(ctx.full_table_name())

                if (!inputTables.contains(tableId) && !cteTempTables.contains(tableId)) {
                    inputTables.add(tableId)
                }
            } else {
                super.visitTable_source_item(ctx)
            }
        }
        return null
    }

    fun parseTableName(ctx: SqlServerParser.Full_table_nameContext): TableId {
        if (ctx.database != null) {
            return TableId(ctx.database.text, ctx.schema.text, ctx.table.text)
        } else if (ctx.schema != null) {
            return TableId(null, ctx.schema.text, ctx.table.text)
        } else {
            return TableId(null, null, ctx.table.text)
        }
    }
}
