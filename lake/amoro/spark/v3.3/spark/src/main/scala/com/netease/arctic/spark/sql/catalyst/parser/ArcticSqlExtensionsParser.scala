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

import java.util.Locale

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.util.Try

import com.netease.arctic.spark.sql.catalyst.plans.UnresolvedMergeIntoArcticTable
import com.netease.arctic.spark.sql.parser._
import com.netease.arctic.spark.table.ArcticSparkTable
import com.netease.arctic.spark.util.ArcticSparkUtils
import org.antlr.v4.runtime._
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.{Interval, ParseCancellationException}
import org.apache.iceberg.spark.Spark3Util
import org.apache.iceberg.spark.source.SparkTable
import org.apache.spark.sql.{AnalysisException, SparkSession}
import org.apache.spark.sql.arctic.parser.ArcticSqlExtendAstBuilder
import org.apache.spark.sql.catalyst.{FunctionIdentifier, SQLConfHelper, TableIdentifier}
import org.apache.spark.sql.catalyst.analysis.{EliminateSubqueryAliases, UnresolvedRelation}
import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.parser.{ParseException, ParserInterface}
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.catalyst.trees.Origin
import org.apache.spark.sql.connector.catalog.{Table, TableCatalog}
import org.apache.spark.sql.types.{DataType, StructType}

class ArcticSqlExtensionsParser(delegate: ParserInterface) extends ParserInterface
  with SQLConfHelper {

  private lazy val createTableAstBuilder = new ArcticSqlExtendAstBuilder()
  private lazy val arcticCommandAstVisitor = new ArcticCommandAstParser()

  /**
   * Parse a string to a DataType.
   */
  override def parseDataType(sqlText: String): DataType = {
    delegate.parseDataType(sqlText)
  }

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
   * Parse a string to a multi-part identifier.
   */
  override def parseMultipartIdentifier(sqlText: String): Seq[String] = {
    delegate.parseMultipartIdentifier(sqlText)
  }

  /**
   * Creates StructType for a given SQL string, which is a comma separated list of field
   * definitions which will preserve the correct Hive metadata.
   */
  override def parseTableSchema(sqlText: String): StructType = {
    delegate.parseTableSchema(sqlText)
  }

  def isArcticCommand(sqlText: String): Boolean = {
    val normalized = sqlText.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ")
    (normalized.contains("migrate") && normalized.contains("to arctic"))
  }

  private val arcticExtendSqlFilters: Seq[String => Boolean] = Seq(
    s => s.contains("create table") && s.contains("primary key"),
    s => s.contains("create temporary table") && s.contains("primary key"))

  private def isArcticExtendSql(sqlText: String): Boolean = {
    val normalized = sqlText.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ")
    arcticExtendSqlFilters.exists(f => f(normalized))
  }

  def buildLexer(sql: String): Option[Lexer] = {
    lazy val charStream = new UpperCaseCharStream(CharStreams.fromString(sql))
    if (isArcticExtendSql(sql)) {
      Some(new ArcticSqlExtendLexer(charStream))
    } else if (isArcticCommand(sql)) {
      Some(new ArcticSqlCommandLexer(charStream))
    } else {
      Option.empty
    }
  }

  def buildAntlrParser(stream: TokenStream, lexer: Lexer): Parser = {
    lexer match {
      case _: ArcticSqlExtendLexer =>
        val parser = new ArcticSqlExtendParser(stream)
        parser.legacy_exponent_literal_as_decimal_enabled = conf.exponentLiteralAsDecimalEnabled
        parser.SQL_standard_keyword_behavior = conf.ansiEnabled
        parser
      case _: ArcticSqlCommandLexer =>
        val parser = new ArcticSqlCommandParser(stream)
        parser
      case _ =>
        throw new IllegalStateException("no suitable parser found")
    }
  }

  def toLogicalResult(parser: Parser): LogicalPlan = parser match {
    case p: ArcticSqlExtendParser =>
      createTableAstBuilder.visitExtendStatement(p.extendStatement())
    case p: ArcticSqlCommandParser =>
      arcticCommandAstVisitor.visitArcticCommand(p.arcticCommand())
  }

  /**
   * Parse a string to a LogicalPlan.
   */
  override def parsePlan(sqlText: String): LogicalPlan = {
    val lexerOpt = buildLexer(sqlText)
    if (lexerOpt.isDefined) {
      val lexer = lexerOpt.get
      lexer.removeErrorListeners()

      val tokenStream = new CommonTokenStream(lexer)
      val parser = buildAntlrParser(tokenStream, lexer)
      parser.removeErrorListeners()

      try {
        try {
          // first, try parsing with potentially faster SLL mode
          parser.getInterpreter.setPredictionMode(PredictionMode.SLL)
          toLogicalResult(parser)
        } catch {
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
      val parsedPlan = delegate.parsePlan(sqlText)
      parsedPlan match {
        case p =>
          replaceMergeIntoCommands(p)
      }
    }
  }

  private def replaceMergeIntoCommands(plan: LogicalPlan): LogicalPlan = plan resolveOperatorsDown {

    case MergeIntoTable(
          UnresolvedArcticTable(aliasedTable),
          source,
          cond,
          matchedActions,
          notMatchedActions) =>
      UnresolvedMergeIntoArcticTable(aliasedTable, source, cond, matchedActions, notMatchedActions)

    case DeleteFromTable(UnresolvedIcebergTable(aliasedTable), condition) =>
      DeleteFromIcebergTable(aliasedTable, Some(condition))

    case UpdateTable(UnresolvedIcebergTable(aliasedTable), assignments, condition) =>
      UpdateIcebergTable(aliasedTable, assignments, condition)

    case MergeIntoTable(
          UnresolvedIcebergTable(aliasedTable),
          source,
          cond,
          matchedActions,
          notMatchedActions) =>
      // cannot construct MergeIntoIcebergTable right away as MERGE operations require special resolution
      // that's why the condition and actions must be hidden from the regular resolution rules in Spark
      // see ResolveMergeIntoTableReferences for details
      val context = MergeIntoContext(cond, matchedActions, notMatchedActions)
      UnresolvedMergeIntoIcebergTable(aliasedTable, source, context)
  }

  object UnresolvedIcebergTable {

    def unapply(plan: LogicalPlan): Option[LogicalPlan] = {
      EliminateSubqueryAliases(plan) match {
        case UnresolvedRelation(multipartIdentifier, _, _) if isIcebergTable(multipartIdentifier) =>
          Some(plan)
        case _ =>
          None
      }
    }

    private def isIcebergTable(multipartIdent: Seq[String]): Boolean = {
      val catalogAndIdentifier =
        Spark3Util.catalogAndIdentifier(SparkSession.active, multipartIdent.asJava)
      catalogAndIdentifier.catalog match {
        case tableCatalog: TableCatalog =>
          Try(tableCatalog.loadTable(catalogAndIdentifier.identifier))
            .map(isIcebergTable)
            .getOrElse(false)

        case _ =>
          false
      }
    }

    private def isIcebergTable(table: Table): Boolean = table match {
      case _: SparkTable => true
      case _ => false
    }
  }

  object UnresolvedArcticTable {

    def unapply(plan: LogicalPlan): Option[LogicalPlan] = {
      EliminateSubqueryAliases(plan) match {
        case UnresolvedRelation(multipartIdentifier, _, _)
            if isArcticKeyedTable(multipartIdentifier) =>
          Some(plan)
        case _ =>
          None
      }
    }

    private def isArcticKeyedTable(multipartIdent: Seq[String]): Boolean = {
      val catalogAndIdentifier =
        ArcticSparkUtils.tableCatalogAndIdentifier(SparkSession.active, multipartIdent.asJava)
      catalogAndIdentifier.catalog match {
        case tableCatalog: TableCatalog =>
          Try(tableCatalog.loadTable(catalogAndIdentifier.identifier))
            .map(isArcticKeyedTable)
            .getOrElse(false)

        case _ =>
          false
      }
    }

    private def isArcticKeyedTable(table: Table): Boolean = table match {
      case _: ArcticSparkTable =>
        true
      case _ => false
    }
  }

  override def parseQuery(sqlText: String): LogicalPlan = parsePlan(sqlText)
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
