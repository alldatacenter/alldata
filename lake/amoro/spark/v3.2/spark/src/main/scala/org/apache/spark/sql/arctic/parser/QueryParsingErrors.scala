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

package org.apache.spark.sql.arctic.parser

import com.netease.arctic.spark.sql.parser.ArcticSqlExtendParser._
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.spark.sql.catalyst.parser.ParseException

/**
 * Object for grouping all error messages of the query parsing.
 * Currently it includes all ParseException.
 */
private[sql] object QueryParsingErrors {


  def columnAliasInOperationNotAllowedError(op: String, ctx: TableAliasContext): Throwable = {
    new ParseException(s"Columns aliases are not allowed in $op.", ctx.identifierList())
  }


  def combinationQueryResultClausesUnsupportedError(ctx: QueryOrganizationContext): Throwable = {
    new ParseException(
      "Combination of ORDER BY/SORT BY/DISTRIBUTE BY/CLUSTER BY is not supported",
      ctx)
  }

  def distributeByUnsupportedError(ctx: QueryOrganizationContext): Throwable = {
    new ParseException("DISTRIBUTE BY is not supported", ctx)
  }

  def transformNotSupportQuantifierError(ctx: ParserRuleContext): Throwable = {
    new ParseException("TRANSFORM does not support DISTINCT/ALL in inputs", ctx)
  }

  def transformWithSerdeUnsupportedError(ctx: ParserRuleContext): Throwable = {
    new ParseException("TRANSFORM with serde is only supported in hive mode", ctx)
  }

  def lateralWithPivotInFromClauseNotAllowedError(ctx: FromClauseContext): Throwable = {
    new ParseException("LATERAL cannot be used together with PIVOT in FROM clause", ctx)
  }

  def lateralJoinWithNaturalJoinUnsupportedError(ctx: ParserRuleContext): Throwable = {
    new ParseException("LATERAL join with NATURAL join is not supported", ctx)
  }

  def lateralJoinWithUsingJoinUnsupportedError(ctx: ParserRuleContext): Throwable = {
    new ParseException("LATERAL join with USING join is not supported", ctx)
  }

  def unsupportedLateralJoinTypeError(ctx: ParserRuleContext, joinType: String): Throwable = {
    new ParseException(s"Unsupported LATERAL join type $joinType", ctx)
  }

  def invalidLateralJoinRelationError(ctx: RelationPrimaryContext): Throwable = {
    new ParseException(s"LATERAL can only be used with subquery", ctx)
  }

  def repetitiveWindowDefinitionError(name: String, ctx: WindowClauseContext): Throwable = {
    new ParseException(s"The definition of window '$name' is repetitive", ctx)
  }

  def invalidWindowReferenceError(name: String, ctx: WindowClauseContext): Throwable = {
    new ParseException(s"Window reference '$name' is not a window specification", ctx)
  }

  def cannotResolveWindowReferenceError(name: String, ctx: WindowClauseContext): Throwable = {
    new ParseException(s"Cannot resolve window reference '$name'", ctx)
  }


  def naturalCrossJoinUnsupportedError(ctx: RelationContext): Throwable = {
    new ParseException("NATURAL CROSS JOIN is not supported", ctx)
  }

  def emptyInputForTableSampleError(ctx: ParserRuleContext): Throwable = {
    new ParseException("TABLESAMPLE does not accept empty inputs.", ctx)
  }

  def tableSampleByBytesUnsupportedError(msg: String, ctx: SampleMethodContext): Throwable = {
    new ParseException(s"TABLESAMPLE($msg) is not supported", ctx)
  }

  def invalidByteLengthLiteralError(bytesStr: String, ctx: SampleByBytesContext): Throwable = {
    new ParseException(
      s"$bytesStr is not a valid byte length literal, " +
        "expected syntax: DIGIT+ ('B' | 'K' | 'M' | 'G')",
      ctx)
  }


  def invalidFromToUnitValueError(ctx: IntervalValueContext): Throwable = {
    new ParseException("The value of from-to unit must be a string", ctx)
  }

  def storedAsAndStoredByBothSpecifiedError(ctx: CreateFileFormatContext): Throwable = {
    new ParseException("Expected either STORED AS or STORED BY, not both", ctx)
  }


  def invalidEscapeStringError(ctx: PredicateContext): Throwable = {
    new ParseException("Invalid escape string. Escape string must contain only one character.", ctx)
  }

  def trimOptionUnsupportedError(trimOption: Int, ctx: TrimContext): Throwable = {
    new ParseException(
      "Function trim doesn't support with " +
        s"type $trimOption. Please use BOTH, LEADING or TRAILING as trim type",
      ctx)
  }

  def invalidIntervalFormError(value: String, ctx: MultiUnitsIntervalContext): Throwable = {
    new ParseException("Can only use numbers in the interval value part for" +
      s" multiple unit value pairs interval form, but got invalid value: $value", ctx)
  }

  def functionNameUnsupportedError(functionName: String, ctx: ParserRuleContext): Throwable = {
    new ParseException(s"Unsupported function name '$functionName'", ctx)
  }

  def cannotParseValueTypeError(
      valueType: String,
      value: String,
      ctx: TypeConstructorContext): Throwable = {
    new ParseException(s"Cannot parse the $valueType value: $value", ctx)
  }

  def cannotParseIntervalValueError(value: String, ctx: TypeConstructorContext): Throwable = {
    new ParseException(s"Cannot parse the INTERVAL value: $value", ctx)
  }

  def literalValueTypeUnsupportedError(
      valueType: String,
      ctx: TypeConstructorContext): Throwable = {
    new ParseException(s"Literals of type '$valueType' are currently not supported.", ctx)
  }

  def parsingValueTypeError(
      e: IllegalArgumentException,
      valueType: String,
      ctx: TypeConstructorContext): Throwable = {
    val message = Option(e.getMessage).getOrElse(s"Exception parsing $valueType")
    new ParseException(message, ctx)
  }

  def invalidNumericLiteralRangeError(
      rawStrippedQualifier: String,
      minValue: BigDecimal,
      maxValue: BigDecimal,
      typeName: String,
      ctx: NumberContext): Throwable = {
    new ParseException(
      s"Numeric literal $rawStrippedQualifier does not " +
        s"fit in range [$minValue, $maxValue] for type $typeName",
      ctx)
  }

  def moreThanOneFromToUnitInIntervalLiteralError(ctx: ParserRuleContext): Throwable = {
    new ParseException("Can only have a single from-to unit in the interval literal syntax", ctx)
  }

  def invalidIntervalLiteralError(ctx: IntervalContext): Throwable = {
    new ParseException("at least one time unit should be given for interval literal", ctx)
  }


  def fromToIntervalUnsupportedError(
      from: String,
      to: String,
      ctx: ParserRuleContext): Throwable = {
    new ParseException(s"Intervals FROM $from TO $to are not supported.", ctx)
  }

  def mixedIntervalUnitsError(literal: String, ctx: ParserRuleContext): Throwable = {
    new ParseException(s"Cannot mix year-month and day-time fields: $literal", ctx)
  }

  def dataTypeUnsupportedError(dataType: String, ctx: PrimitiveDataTypeContext): Throwable = {
    new ParseException(s"DataType $dataType is not supported.", ctx)
  }

  def partitionTransformNotExpectedError(
      name: String,
      describe: String,
      ctx: ApplyTransformContext): Throwable = {
    new ParseException(s"Expected a column reference for transform $name: $describe", ctx)
  }

  def tooManyArgumentsForTransformError(name: String, ctx: ApplyTransformContext): Throwable = {
    new ParseException(s"Too many arguments for transform $name", ctx)
  }


  def invalidBucketsNumberError(describe: String, ctx: ApplyTransformContext): Throwable = {
    new ParseException(s"Invalid number of buckets: $describe", ctx)
  }


  def cannotCleanReservedNamespacePropertyError(
      property: String,
      ctx: ParserRuleContext,
      msg: String): Throwable = {
    new ParseException(s"$property is a reserved namespace property, $msg.", ctx)
  }


  def cannotCleanReservedTablePropertyError(
      property: String,
      ctx: ParserRuleContext,
      msg: String): Throwable = {
    new ParseException(s"$property is a reserved table property, $msg.", ctx)
  }

  def duplicatedTablePathsFoundError(
      pathOne: String,
      pathTwo: String,
      ctx: ParserRuleContext): Throwable = {
    new ParseException(
      s"Duplicated table paths found: '$pathOne' and '$pathTwo'. LOCATION" +
        s" and the case insensitive key 'path' in OPTIONS are all used to indicate the custom" +
        s" table path, you can only specify one of them.",
      ctx)
  }

  def operationNotAllowedError(message: String, ctx: ParserRuleContext): Throwable = {
    new ParseException(s"Operation not allowed: $message", ctx)
  }

  def duplicateCteDefinitionNamesError(duplicateNames: String, ctx: CtesContext): Throwable = {
    new ParseException(s"CTE definition can't have duplicate names: $duplicateNames.", ctx)
  }

  def invalidGroupingSetError(element: String, ctx: GroupingAnalyticsContext): Throwable = {
    new ParseException(s"Empty set in $element grouping sets is not supported.", ctx)
  }

}
