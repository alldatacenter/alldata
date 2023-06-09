package com.platform.antlr.parser.flink

import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.relational.DefaultStatement
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.table.ColumnRel
import com.platform.antlr.parser.common.util.StringUtil
import com.platform.antlr.parser.flink.antlr4.FlinkCdcSqlParser
import com.platform.antlr.parser.flink.antlr4.FlinkCdcSqlParserBaseVisitor
import org.antlr.v4.runtime.tree.RuleNode
import org.apache.commons.lang3.StringUtils

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class FlinkSQLAntlr4Visitor : FlinkCdcSqlParserBaseVisitor<Statement>() {
    private var currentOptType: StatementType = StatementType.UNKOWN
    private var command: String? = null

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Statement?): Boolean {
        return if (currentResult == null) true else false
    }

    override fun visitSingleStatement(ctx: FlinkCdcSqlParser.SingleStatementContext): Statement {
        val data = super.visitSingleStatement(ctx)

        if (data == null) {
            throw SQLParserException("不支持的SQL: " + command)
        }

        return data
    }

    override fun visitBeginStatement(ctx: FlinkCdcSqlParser.BeginStatementContext): Statement {
        return DefaultStatement(StatementType.FLINK_CDC_BEGIN)
    }

    override fun visitEndStatement(ctx: FlinkCdcSqlParser.EndStatementContext): Statement {
        currentOptType = StatementType.FLINK_CDC_CTAS
        return DefaultStatement(StatementType.FLINK_CDC_END)
    }

    override fun visitCreateTable(ctx: FlinkCdcSqlParser.CreateTableContext): Statement {
        val sinkTable = parseTable(ctx.sink)
        val sourceTable = parseTable(ctx.source)

        val sinkOptions: HashMap<String, String>? = parseOptions(ctx.sinkOptions)
        val sourceOptions: HashMap<String, String>? = parseOptions(ctx.sourceOptions)

        val createTable = FlinkCdcCreateTable(sinkTable, sourceTable)
        createTable.sinkOptions = sinkOptions
        createTable.sourceOptions = sourceOptions

        if (ctx.computeColList() != null) {
            val colDefs = ctx.computeColList().computeColDef();
            val columns = colDefs.map { colDef ->
                val colName = colDef.colName.text
                val columnRel = ColumnRel(colName)

                val expression = colDef.expression();
                val expr = StringUtils.substring(command, expression.start.startIndex, expression.stop.stopIndex + 1)
                columnRel.expression = expr

                if (colDef.FIRST() != null) {
                    columnRel.position = "FIRST"
                }
                if (colDef.AFTER() != null) {
                    columnRel.position = "AFTER"
                    columnRel.afterCol = colDef.name.text
                }

                columnRel
            }

            createTable.computeCols = columns
        }

        return createTable
    }

    override fun visitCreateDatabase(ctx: FlinkCdcSqlParser.CreateDatabaseContext): Statement {
        val sinkDatabase = parseDatabase(ctx.sink)
        val sourceDatabase = parseDatabase(ctx.source)

        val sinkOptions: HashMap<String, String>? = parseOptions(ctx.sinkOptions)
        val sourceOptions: HashMap<String, String>? = parseOptions(ctx.sourceOptions)

        val createDatabase = if (ctx.includeTable == null) {
            FlinkCdcCreateDatabase(sinkDatabase.first, sinkDatabase.second, sourceDatabase.first, sourceDatabase.second, "__ALL__")
        } else {
            FlinkCdcCreateDatabase(sinkDatabase.first, sinkDatabase.second, sourceDatabase.first, sourceDatabase.second, StringUtil.cleanQuote(ctx.includeTable.text))
        }

        if (ctx.excludeTable != null) {
            createDatabase.excludeTable = StringUtil.cleanQuote(ctx.excludeTable.text)
        }

        createDatabase.sinkOptions = sinkOptions
        createDatabase.sourceOptions = sourceOptions
        return createDatabase
    }

    fun parseDatabase(ctx: FlinkCdcSqlParser.MultipartIdentifierContext): Pair<String?, String> {
        if (ctx.parts.size == 2) {
            return Pair(ctx.parts.get(0).text, ctx.parts.get(1).text)
        } else if (ctx.parts.size == 1) {
            return Pair(null, ctx.parts.get(0).text)
        } else {
            throw SQLParserException("parse multipart error: " + ctx.parts.size)
        }
    }

    fun parseTable(ctx: FlinkCdcSqlParser.MultipartIdentifierContext): TableId {
        if (ctx.parts.size == 4) {
            return TableId(ctx.parts.get(0).text, ctx.parts.get(1).text, ctx.parts.get(2).text, ctx.parts.get(3).text)
        } else if (ctx.parts.size == 3) {
            return TableId(ctx.parts.get(0).text, ctx.parts.get(1).text, ctx.parts.get(2).text)
        } else if (ctx.parts.size == 2) {
            return TableId(null, ctx.parts.get(0).text, ctx.parts.get(1).text)
        } else if (ctx.parts.size == 1) {
            return TableId(null, null, ctx.parts.get(0).text)
        } else {
            throw SQLParserException("parse multipart error: " + ctx.parts.size)
        }
    }

    fun parseOptions(options: FlinkCdcSqlParser.PropertyListContext?): HashMap<String, String>? {
        if (options == null) {
            return null
        }
        val properties = HashMap<String, String>()
        options.children.filter { it is FlinkCdcSqlParser.PropertyContext }.map { item ->
            val property = item as FlinkCdcSqlParser.PropertyContext
            val key = StringUtil.cleanQuote(property.key.text)
            val value = StringUtil.cleanQuote(property.value.text)
            properties.put(key, value)
        }
        return properties
    }

    fun setCommand(command: String) {
        this.command = command;
    }
}