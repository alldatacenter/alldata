package com.platform.antlr.parser.spark

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.StatementType
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils
import com.platform.antlr.parser.spark.antlr4.SparkStreamSqlLexer
import com.platform.antlr.parser.spark.antlr4.SparkStreamSqlParser

object SparkStreamSqlHelper {

    @JvmStatic fun checkSupportedSQL(statementType: StatementType): Boolean {
        return when (statementType) {
            StatementType.CREATE_TABLE,
            StatementType.SET,
            StatementType.INSERT
            -> true
            else -> false
        }
    }

    @JvmStatic fun getStatement(command: String) : ArrayList<Statement> {
        val trimCmd = StringUtils.trim(command)

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(trimCmd))
        val lexer = SparkStreamSqlLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = SparkStreamSqlParser(tokenStream)
        parser.addParseListener(SparkSqlPostProcessor())
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())
        parser.interpreter.predictionMode = PredictionMode.SLL

        val sqlVisitor = SparkStreamSqlAntlr4Visitor()
        sqlVisitor.setCommand(trimCmd)
        try {
            try {
                // first, try parsing with potentially faster SLL mode
                sqlVisitor.visit(parser.sqlStatements())
                return sqlVisitor.getTableDatas()
            }
            catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                sqlVisitor.visit(parser.sqlStatements())
                return sqlVisitor.getTableDatas()
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
