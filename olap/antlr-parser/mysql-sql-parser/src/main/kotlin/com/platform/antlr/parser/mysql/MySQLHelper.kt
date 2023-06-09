package com.platform.antlr.parser.mysql

import com.platform.antlr.parser.common.relational.Statement
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import org.apache.commons.lang3.StringUtils
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.DefaultStatement
import com.platform.antlr.parser.mysql.antlr4.MySqlLexer
import com.platform.antlr.parser.mysql.antlr4.MySqlParser

/**
 *
 * Created by libinsong on 2018/1/10.
 */
object MySQLHelper {

    @JvmStatic fun checkSupportedSQL(statementType: StatementType?): Boolean {
        return when (statementType) {
            StatementType.CREATE_TABLE,
            StatementType.DROP_TABLE,
            StatementType.TRUNCATE_TABLE,
            StatementType.ALTER_TABLE,

            StatementType.INSERT,
            StatementType.SELECT,
            StatementType.DELETE,
            StatementType.UPDATE,
            StatementType.ANALYZE_TABLE,
            StatementType.SHOW
            -> true
            else -> false
        }
    }

    @JvmStatic fun getStatement(command: String): Statement {
        val trimCmd = StringUtils.trim(command)

        if (StringUtils.startsWithIgnoreCase(trimCmd,"show")) {
            return DefaultStatement(StatementType.SHOW)
        }

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(trimCmd))
        val lexer = MySqlLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = MySqlParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val sqlVisitor = MySQLAntlr4Visitor()
        try {
            try {
                // first, try parsing with potentially faster SLL mode
                val data = sqlVisitor.visit(parser.sqlStatement())
                if (data == null) {
                    return DefaultStatement(StatementType.UNKOWN)
                } else {
                    return data
                }
            }
            catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                val data = sqlVisitor.visit(parser.sqlStatement())
                if (data == null) {
                    return DefaultStatement(StatementType.UNKOWN)
                } else {
                    return data
                }
            }
        } catch (e: com.platform.antlr.parser.common.antlr4.ParseException) {
            if(StringUtils.isNotBlank(e.command)) {
                throw e;
            } else {
                throw e.withCommand(trimCmd)
            }
        }
    }

    @JvmStatic fun splitAlterSql(sql: String): List<String> {
        val _items = mutableListOf<String>()

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(sql))
        val lexer = MySqlLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokens = CommonTokenStream(lexer)
        val parser = MySqlParser(tokens)
        val statement = parser.sqlStatement().getChild(0).getChild(0)

        if (statement is MySqlParser.AlterTableContext) {
            val tableNameContext = statement.getChild(2) as MySqlParser.TableNameContext
            val stopIndex = tableNameContext.stop.stopIndex
            val alterSqlPrefix = StringUtils.substring(sql, 0, stopIndex + 1);

            val childStat = statement.getChild(3)
            if (childStat is MySqlParser.AlterByAddColumnsContext) {
                val child = childStat.getChild(2)
                var startIndex = if(child is MySqlParser.UidContext) {
                    val rightNode = child.getChild(0)
                    if (rightNode is TerminalNodeImpl) {
                        rightNode.symbol.startIndex
                    } else {
                        val childNode = rightNode.getChild(0)
                        if (childNode is MySqlParser.EngineNameContext) {
                            childNode.start.startIndex
                        } else {
                            val node = rightNode.getChild(0) as TerminalNodeImpl
                            node.symbol.startIndex
                        }
                    }
                } else {
                    val right = child as TerminalNodeImpl
                    right.symbol.startIndex + 1
                }

                for(i in 3 .. childStat.childCount) {
                    val node = childStat.getChild(i)
                    if(node is TerminalNodeImpl) {
                        if("," == node.text) {
                            val alterSql = StringUtils.substring(sql, startIndex, node.symbol.startIndex)
                            _items.add(alterSqlPrefix + " ADD COLUMN " + alterSql)
                            startIndex = node.symbol.startIndex + 1
                        }
                    }
                }

                var alterSql = StringUtils.substring(sql, startIndex)
                alterSql = StringUtils.substringBeforeLast(alterSql, ")")
                _items.add(alterSqlPrefix + " ADD COLUMN " + alterSql)

            } else {
                val childCount = statement.childCount;

                var index = 3
                while (index < childCount) {
                    val startContext = statement.getChild(index) as ParserRuleContext
                    index = index + 1
                    val stopContext:TerminalNodeImpl?
                            = if(index<childCount) statement.getChild(index) as TerminalNodeImpl else null

                    val startIndex = startContext.start.startIndex
                    val stopIndex = if (stopContext != null) stopContext.symbol.startIndex else -1;

                    var sql = if(stopIndex > 0) {
                        StringUtils.substring(sql, startIndex, stopIndex)
                    } else {
                        StringUtils.substring(sql, startIndex)
                    }

                    sql = StringUtils.trim(sql)
                    if (StringUtils.endsWith(sql, ";")) {
                        sql = StringUtils.substring(sql, 0, -1)
                    }

                    _items.add(alterSqlPrefix + " " + sql)

                    index = index + 1
                }
            }
        }

        return _items.toList()
    }
}
