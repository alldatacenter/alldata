/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.sql.catalyst.parser

import com.netease.arctic.spark.sql.parser.ArcticSparkSqlParser.{NonReservedContext, QuotedIdentifierContext}
import com.netease.arctic.spark.sql.parser.{ArcticSparkSqlBaseListener, ArcticSparkSqlLexer, ArcticSparkSqlParser}
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.{Interval, ParseCancellationException}
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import org.apache.spark.sql.AnalysisException
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.parser.{ParseException, ParserInterface}
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.trees.Origin
import org.apache.spark.sql.catalyst.{FunctionIdentifier, TableIdentifier}
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.types.{DataType, StructType}

import java.util.Locale

class ArcticSqlExtensionsParser(delegate: ParserInterface) extends ParserInterface {

  private lazy val createTableAstBuilder = new ArcticExtendSparkSqlAstBuilder(new SQLConf())

  /**
   * Parse a string to a DataType.
   */
  override def parseDataType(sqlText: String): DataType = {
    delegate.parseDataType(sqlText)
  }

  /**
   * Parse a string to a raw DataType without CHAR/VARCHAR replacement.
   */
  def parseRawDataType(sqlText: String): DataType = throw new UnsupportedOperationException()

  /**
   * Parse a string to an Expression.
   */
  override def parseExpression(sqlText: String): Expression = {
    delegate.parseExpression(sqlText)
  }

  /**
   * Parse a string to a TableIdentifier.
   */
  override def parseTableIdentifier(sqlText: String): TableIdentifier = {
    delegate.parseTableIdentifier(sqlText)
  }

  /**
   * Parse a string to a FunctionIdentifier.
   */
  override def parseFunctionIdentifier(sqlText: String): FunctionIdentifier = {
    delegate.parseFunctionIdentifier(sqlText)
  }

  /**
   * Creates StructType for a given SQL string, which is a comma separated list of field
   * definitions which will preserve the correct Hive metadata.
   */
  override def parseTableSchema(sqlText: String): StructType = {
    delegate.parseTableSchema(sqlText)
  }

  def isArcticExtendSparkStatement(sqlText: String): Boolean = {
    val normalized = sqlText.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ")
     normalized.contains("create table") && normalized.contains("using arctic")
  }

  def buildLexer(sql: String): Option[Lexer] = {
    lazy val charStream = new UpperCaseCharStream(CharStreams.fromString(sql))
    if (isArcticExtendSparkStatement(sql)) {
      Some(new ArcticSparkSqlLexer(charStream))
    } else {
      Option.empty
    }
  }

  def buildAntlrParser(stream: TokenStream, lexer: Lexer): Parser = {
    lexer match {
      case _: ArcticSparkSqlLexer =>
        val parser = new ArcticSparkSqlParser(stream)
        parser
      case _ =>
        throw new IllegalStateException("no suitable parser found")
    }
  }

  def toLogicalResult(parser: Parser): LogicalPlan = parser match {
    case p: ArcticSparkSqlParser =>
      createTableAstBuilder.visitSingleStatement(p.singleStatement())
  }

  /**
   * Parse a string to a LogicalPlan.
   */
  override def parsePlan(sqlText: String): LogicalPlan = {
    val lexerOpt = buildLexer(sqlText)
    if (lexerOpt.isDefined) {
      val lexer = lexerOpt.get
      lexer.removeErrorListeners()
      lexer.addErrorListener(ArcticParseErrorListener)

      val tokenStream = new CommonTokenStream(lexer)
      val parser = buildAntlrParser(tokenStream, lexer)
      parser.removeErrorListeners()
      parser.addErrorListener(ArcticParseErrorListener)

      try {
        try {
          // first, try parsing with potentially faster SLL mode
          parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
          toLogicalResult(parser)
        }
        catch {
          case _: ParseCancellationException =>
            // if we fail, parse with LL mode
            tokenStream.seek(0) // rewind input stream
            parser.reset()
            // Try Again.
            parser.getInterpreter.setPredictionMode(PredictionMode.LL)
            toLogicalResult(parser)
        }
      } catch {
        case e: ParseException if e.command.isDefined =>
          throw e
        case e: ParseException => throw e.withCommand(sqlText)
        case e: AnalysisException =>
          val position = Origin(e.line, e.startPosition)
          throw new ParseException(Option(sqlText), e.message, position, position)
      }
    } else {
      delegate.parsePlan(sqlText)
    }
  }

}

/* Copied from Apache Spark's to avoid dependency on Spark Internals */
class UpperCaseCharStream(wrapped: CodePointCharStream) extends CharStream {
  override def consume(): Unit = wrapped.consume

  override def getSourceName(): String = wrapped.getSourceName

  override def index(): Int = wrapped.index

  override def mark(): Int = wrapped.mark

  override def release(marker: Int): Unit = wrapped.release(marker)

  override def seek(where: Int): Unit = wrapped.seek(where)

  override def size(): Int = wrapped.size

  override def getText(interval: Interval): String = wrapped.getText(interval)

  // scalastyle:off
  override def LA(i: Int): Int = {
    val la = wrapped.LA(i)
    if (la == 0 || la == IntStream.EOF) la
    else Character.toUpperCase(la)
  }
  // scalastyle:on
}

/**
 * The post-processor validates & cleans-up the parse tree during the parse process.
 */
case object ArcticSqlExtensionsPostProcessor extends ArcticSparkSqlBaseListener {

  /** Remove the back ticks from an Identifier. */
  override def exitQuotedIdentifier(ctx: QuotedIdentifierContext): Unit = {
    replaceTokenByIdentifier(ctx, 1) { token =>
      // Remove the double back ticks in the string.
      token.setText(token.getText.replace("``", "`"))
      token
    }
  }

  /** Treat non-reserved keywords as Identifiers. */
  override def exitNonReserved(ctx: NonReservedContext): Unit = {
    replaceTokenByIdentifier(ctx, 0)(identity)
  }

  private def replaceTokenByIdentifier(
                                        ctx: ParserRuleContext,
                                        stripMargins: Int)(
                                        f: CommonToken => CommonToken = identity): Unit = {
    val parent = ctx.getParent
    parent.removeLastChild()
    val token = ctx.getChild(0).getPayload.asInstanceOf[Token]
    val newToken = new CommonToken(
      new org.antlr.v4.runtime.misc.Pair(token.getTokenSource, token.getInputStream),
      ArcticSparkSqlParser.IDENTIFIER,
      token.getChannel,
      token.getStartIndex + stripMargins,
      token.getStopIndex - stripMargins)
    parent.addChild(new TerminalNodeImpl(f(newToken)))
  }
}

/* Partially copied from Apache Spark's Parser to avoid dependency on Spark Internals */
case object ArcticParseErrorListener extends BaseErrorListener {
  override def syntaxError(
                            recognizer: Recognizer[_, _],
                            offendingSymbol: scala.Any,
                            line: Int,
                            charPositionInLine: Int,
                            msg: String,
                            e: RecognitionException): Unit = {
    val (start, stop) = offendingSymbol match {
      case token: CommonToken =>
        val start = Origin(Some(line), Some(token.getCharPositionInLine))
        val length = token.getStopIndex - token.getStartIndex + 1
        val stop = Origin(Some(line), Some(token.getCharPositionInLine + length))
        (start, stop)
      case _ =>
        val start = Origin(Some(line), Some(charPositionInLine))
        (start, start)
    }
  }
}


