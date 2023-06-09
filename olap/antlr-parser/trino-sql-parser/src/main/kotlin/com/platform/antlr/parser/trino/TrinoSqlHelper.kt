package com.platform.antlr.parser.trino

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.common.StatementType
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils
import com.platform.antlr.parser.trino.antlr4.TrinoSqlBaseLexer
import com.platform.antlr.parser.trino.antlr4.TrinoSqlBaseParser

/**
 *
 * Created by libinsong on 2018/1/10.
 */
object TrinoSqlHelper {

    @JvmStatic fun checkSupportedSQL(statementType: StatementType): Boolean {
        return when (statementType) {
            StatementType.SHOW -> true
            StatementType.DROP_TABLE -> true
            StatementType.EXPLAIN -> true
            StatementType.SELECT -> true
            StatementType.CREATE_TABLE_AS_SELECT -> true
            else -> false
        }
    }

    @JvmStatic fun getStatement(command: String) : Statement? {
        val trimCmd = StringUtils.trim(command)

        val charStream = CaseInsensitiveStream(
            CharStreams.fromString(trimCmd)
        )
        val lexer = TrinoSqlBaseLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = TrinoSqlBaseParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())
        parser.interpreter.predictionMode = PredictionMode.SLL

        val sqlVisitor = TrinoSqlAntlr4Visitor()
        sqlVisitor.setCommand(trimCmd)

        try {
            try {
                // first, try parsing with potentially faster SLL mode
                return sqlVisitor.visit(parser.singleStatement())
            }
            catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                return sqlVisitor.visit(parser.singleStatement())
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
