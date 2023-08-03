package com.platform.antlr.parser.spark

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.StatementType.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils
import com.platform.antlr.parser.spark.antlr4.SparkSqlLexer
import com.platform.antlr.parser.spark.antlr4.SparkSqlParser

/**
 *
 * Created by libinsong on 2018/1/10.
 */
object SparkSqlHelper {

    @JvmStatic fun checkSupportedSQL(statementType: StatementType): Boolean {
        return when (statementType) {
            CREATE_DATABASE,
            CREATE_SCHEMA,
            CREATE_TABLE,
            CREATE_TABLE_AS_SELECT,
            CREATE_TABLE_AS_LIKE,
            TRUNCATE_TABLE,
            MERGE,
            REFRESH_TABLE,
            EXPORT_TABLE,
            ANALYZE_TABLE,

            ALTER_TABLE,
            REPAIR_TABLE,

            SELECT,
            INSERT,

            CREATE_FILE_VIEW,
            CREATE_VIEW,
            CREATE_TEMP_VIEW_USING,
            DROP_DATABASE,
            DROP_SCHEMA,
            DROP_VIEW,
            DROP_TABLE,

            SHOW,

            CACHE,
            UNCACHE,
            CLEAR_CACHE,

            DATATUNNEL,
            CALL,
            HELP,
            SYNC,

            DELETE,
            UPDATE,

            EXPLAIN
            -> true
            else -> false
        }
    }

    @JvmStatic fun getStatement(command: String): Statement {
        val trimCmd = StringUtils.trim(command)

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(trimCmd))
        val lexer = SparkSqlLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = SparkSqlParser(tokenStream)
        parser.addParseListener(SparkSqlPostProcessor())
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())
        parser.interpreter.predictionMode = PredictionMode.SLL

        val sqlVisitor = SparkSqlAntlr4Visitor()
        sqlVisitor.setCommand(trimCmd)

        try {
            return try {
                // first, try parsing with potentially faster SLL mode
                sqlVisitor.visit(parser.singleStatement())
            } catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                sqlVisitor.visit(parser.statement())
            }
        } catch (e: com.platform.antlr.parser.common.antlr4.ParseException) {
            if(StringUtils.isNotBlank(e.command)) {
                throw e;
            } else {
                throw e.withCommand(trimCmd)
            }
        }
    }
}
