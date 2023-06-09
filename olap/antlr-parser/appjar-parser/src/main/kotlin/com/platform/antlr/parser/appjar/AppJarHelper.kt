package com.platform.antlr.parser.appjar

import AppJarLexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang3.StringUtils
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.appjar.antlr4.AppJarParser

/**
 *
 * Created by binsong.li on 2018/3/31 下午1:47
 */
object AppJarHelper {

    @JvmStatic fun getStatement(command: String) : ArrayList<Statement> {
        val trimCmd = StringUtils.trim(command)

        val charStream =
            com.platform.antlr.parser.common.antlr4.UpperCaseCharStream(CharStreams.fromString(trimCmd))
        val lexer = AppJarLexer(charStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())

        val tokenStream = CommonTokenStream(lexer)
        val parser = AppJarParser(tokenStream)
        parser.removeErrorListeners()
        parser.addErrorListener(com.platform.antlr.parser.common.antlr4.ParseErrorListener())
        parser.interpreter.predictionMode = PredictionMode.SLL

        val cmdVisitor = AppJarAntlr4Visitor()
        cmdVisitor.setCommand(trimCmd)
        try {
            try {
                // first, try parsing with potentially faster SLL mode
                cmdVisitor.visit(parser.jobTasks())
                return cmdVisitor.getTableDatas()
            }
            catch (e: ParseCancellationException) {
                tokenStream.seek(0) // rewind input stream
                parser.reset()

                // Try Again.
                parser.interpreter.predictionMode = PredictionMode.LL
                cmdVisitor.visit(parser.jobTasks())
                return cmdVisitor.getTableDatas()
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