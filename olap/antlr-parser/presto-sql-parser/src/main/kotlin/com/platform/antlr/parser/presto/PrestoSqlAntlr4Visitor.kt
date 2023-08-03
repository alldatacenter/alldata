package com.platform.antlr.parser.presto

import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.relational.DefaultStatement
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.dml.QueryStmt
import com.platform.antlr.parser.common.relational.create.CreateTableAsSelect
import com.platform.antlr.parser.common.relational.drop.DropTable
import com.platform.antlr.parser.presto.antlr4.PrestoSqlBaseBaseVisitor
import com.platform.antlr.parser.presto.antlr4.PrestoSqlBaseParser
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.RuleNode
import org.apache.commons.lang3.StringUtils

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class PrestoSqlAntlr4Visitor : PrestoSqlBaseBaseVisitor<Statement>() {

    private var currentOptType: StatementType = StatementType.UNKOWN
    private var command: String? = null

    private var limit:Int? = null
    private var inputTables: ArrayList<TableId> = arrayListOf()

    fun setCommand(command: String) {
        this.command = command
    }

    override fun visit(tree: ParseTree?): Statement? {
        val data = super.visit(tree)

        if (data == null) {
            throw SQLParserException("不支持的SQL")
        }

        return data;
    }

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Statement?): Boolean {
        return if (currentResult == null) true else false
    }

    override fun visitStatementDefault(ctx: PrestoSqlBaseParser.StatementDefaultContext): Statement? {
        if (StringUtils.equalsIgnoreCase("select", ctx.start.text)) {
            currentOptType = StatementType.SELECT
            super.visitQuery(ctx.query())

            val limit = ctx.query()?.queryNoWith()?.limit?.text?.toInt()
            return QueryStmt(inputTables, limit)
        } else {
            return null
        }
    }

    override fun visitCreateTableAsSelect(ctx: PrestoSqlBaseParser.CreateTableAsSelectContext): Statement? {
        currentOptType = StatementType.CREATE_TABLE_AS_SELECT
        val tableId = createTableSource(ctx.qualifiedName())
        val createTable = CreateTableAsSelect(tableId)

        var querySql = StringUtils.substring(command, ctx.query().start.startIndex)
        if (StringUtils.startsWith(querySql, "(") && StringUtils.endsWith(querySql, ")")) {
            querySql = StringUtils.substringBetween(querySql, "(", ")")
        }

        createTable.lifeCycle = 7
        createTable.querySql = querySql

        super.visitQuery(ctx.query())

        createTable.inputTables.addAll(inputTables)
        return createTable
    }

    override fun visitDropTable(ctx: PrestoSqlBaseParser.DropTableContext): Statement? {
        val tableId = createTableSource(ctx.qualifiedName())

        val dropTable = DropTable(tableId)
        dropTable.ifExists = ctx.EXISTS() != null
        return dropTable
    }

    override fun visitExplain(ctx: PrestoSqlBaseParser.ExplainContext): Statement? {
        return DefaultStatement(StatementType.EXPLAIN)
    }

    override fun visitQualifiedName(ctx: PrestoSqlBaseParser.QualifiedNameContext): Statement? {
        if (!(ctx.parent is PrestoSqlBaseParser.TableNameContext)) {
            return null
        }

        if (currentOptType == StatementType.SELECT ||
            currentOptType == StatementType.CREATE_TABLE_AS_SELECT) {

            val tableName = createTableSource(ctx)
            inputTables.add(tableName)
        }
        return null
    }

    private fun createTableSource(ctx: PrestoSqlBaseParser.QualifiedNameContext): TableId {
        val list = ctx.identifier()

        var catalogName: String? = null
        var databaseName: String? = null
        val tableName = if (list.size == 1) {
            ctx.text
        } else if (list.size == 2) {
            val index = StringUtils.lastIndexOf(ctx.text, ".")
            databaseName = StringUtils.substring(ctx.text, 0, index)

            StringUtils.substring(ctx.text, index + 1)
        } else {
            val items = StringUtils.split(ctx.text, ".");
            catalogName = items[0];
            databaseName = items[1];
            items[2]
        }

        return TableId(catalogName, databaseName, tableName)
    }
}
