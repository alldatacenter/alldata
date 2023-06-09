package com.platform.antlr.parser.arithmetic

import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.antlr4.arithmetic.ArithmeticLexer
import com.platform.antlr.parser.antlr4.arithmetic.ArithmeticParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils

/**
 *
 * Created by libinsong on 2018/1/10.
 */
object ArithmeticHelper {

    @JvmStatic fun getStatement(command: String) : Statement? {
        return ArithmeticHelper.getStatement(command, true)
    }

    @JvmStatic fun getStatement(command: String, bracketEnbled: Boolean) : Statement? {
        val trimCmd = StringUtils.trim(command)

        val charStream = CharStreams.fromString(trimCmd);
        val lexer = ArithmeticLexer(charStream)

        val tokenStream = CommonTokenStream(lexer)
        val parser = ArithmeticParser(tokenStream)
        parser.bracket_enbled = bracketEnbled
        parser.interpreter.predictionMode = PredictionMode.SLL

        val sqlVisitor = ArithmeticAntlr4Visitor(bracketEnbled)

        try {
            try {
                // first, try parsing with potentially faster SLL mode
                return sqlVisitor.visit(parser.expression())
            } catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                return sqlVisitor.visit(parser.expression())
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
