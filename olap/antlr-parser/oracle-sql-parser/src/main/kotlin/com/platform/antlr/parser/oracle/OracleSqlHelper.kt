package com.platform.antlr.parser.oracle

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.oracle.antlr4.OracleLexer
import com.platform.antlr.parser.oracle.antlr4.OracleParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils

/**
 * Created by libinsong on 2020/6/30 9:58 上午
 */
object OracleSqlHelper {

    @JvmStatic fun checkSupportedSQL(statementType: StatementType): Boolean {
        return when (statementType) {
            StatementType.SELECT -> true
            else -> false
        }
    }

    @JvmStatic fun getStatement(command: String): Statement {
        val trimCmd = StringUtils.trim(command)

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(trimCmd))
        val lexer = OracleLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = OracleParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())
        // parser.interpreter.predictionMode = PredictionMode.SLL

        val sqlVisitor = OracleSqlAntlr4Visitor()

        try {
            try {
                // first, try parsing with potentially faster SLL mode
                return sqlVisitor.visit(parser.sql_script())
            }
            catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                return sqlVisitor.visit(parser.sql_script())
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
