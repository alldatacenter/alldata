// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.flink.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FlinkCdcSqlParser}.
 */
public interface FlinkCdcSqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleStatement(FlinkCdcSqlParser.SingleStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleStatement(FlinkCdcSqlParser.SingleStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code beginStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterBeginStatement(FlinkCdcSqlParser.BeginStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code beginStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitBeginStatement(FlinkCdcSqlParser.BeginStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code endStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterEndStatement(FlinkCdcSqlParser.EndStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code endStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitEndStatement(FlinkCdcSqlParser.EndStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateTable(FlinkCdcSqlParser.CreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateTable(FlinkCdcSqlParser.CreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code createDatabase}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreateDatabase(FlinkCdcSqlParser.CreateDatabaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code createDatabase}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreateDatabase(FlinkCdcSqlParser.CreateDatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(FlinkCdcSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(FlinkCdcSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierList(FlinkCdcSqlParser.IdentifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#identifierList}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierList(FlinkCdcSqlParser.IdentifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierSeq(FlinkCdcSqlParser.IdentifierSeqContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierSeq(FlinkCdcSqlParser.IdentifierSeqContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#stringLit}.
	 * @param ctx the parse tree
	 */
	void enterStringLit(FlinkCdcSqlParser.StringLitContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#stringLit}.
	 * @param ctx the parse tree
	 */
	void exitStringLit(FlinkCdcSqlParser.StringLitContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void enterCommentSpec(FlinkCdcSqlParser.CommentSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 */
	void exitCommentSpec(FlinkCdcSqlParser.CommentSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void enterPropertyList(FlinkCdcSqlParser.PropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#propertyList}.
	 * @param ctx the parse tree
	 */
	void exitPropertyList(FlinkCdcSqlParser.PropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(FlinkCdcSqlParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(FlinkCdcSqlParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 */
	void enterPropertyKey(FlinkCdcSqlParser.PropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 */
	void exitPropertyKey(FlinkCdcSqlParser.PropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 */
	void enterPropertyValue(FlinkCdcSqlParser.PropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 */
	void exitPropertyValue(FlinkCdcSqlParser.PropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#computeColList}.
	 * @param ctx the parse tree
	 */
	void enterComputeColList(FlinkCdcSqlParser.ComputeColListContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#computeColList}.
	 * @param ctx the parse tree
	 */
	void exitComputeColList(FlinkCdcSqlParser.ComputeColListContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#computeColDef}.
	 * @param ctx the parse tree
	 */
	void enterComputeColDef(FlinkCdcSqlParser.ComputeColDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#computeColDef}.
	 * @param ctx the parse tree
	 */
	void exitComputeColDef(FlinkCdcSqlParser.ComputeColDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(FlinkCdcSqlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(FlinkCdcSqlParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(FlinkCdcSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(FlinkCdcSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(FlinkCdcSqlParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(FlinkCdcSqlParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(FlinkCdcSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(FlinkCdcSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(FlinkCdcSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(FlinkCdcSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(FlinkCdcSqlParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(FlinkCdcSqlParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(FlinkCdcSqlParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(FlinkCdcSqlParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cast}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterCast(FlinkCdcSqlParser.CastContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitCast(FlinkCdcSqlParser.CastContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(FlinkCdcSqlParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(FlinkCdcSqlParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(FlinkCdcSqlParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(FlinkCdcSqlParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(FlinkCdcSqlParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(FlinkCdcSqlParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(FlinkCdcSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(FlinkCdcSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(FlinkCdcSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(FlinkCdcSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(FlinkCdcSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(FlinkCdcSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(FlinkCdcSqlParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(FlinkCdcSqlParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterParameterLiteral(FlinkCdcSqlParser.ParameterLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitParameterLiteral(FlinkCdcSqlParser.ParameterLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterTypeConstructor(FlinkCdcSqlParser.TypeConstructorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitTypeConstructor(FlinkCdcSqlParser.TypeConstructorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(FlinkCdcSqlParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(FlinkCdcSqlParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(FlinkCdcSqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(FlinkCdcSqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(FlinkCdcSqlParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(FlinkCdcSqlParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterLegacyDecimalLiteral(FlinkCdcSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitLegacyDecimalLiteral(FlinkCdcSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(FlinkCdcSqlParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(FlinkCdcSqlParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(FlinkCdcSqlParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(FlinkCdcSqlParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(FlinkCdcSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(FlinkCdcSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(FlinkCdcSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(FlinkCdcSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(FlinkCdcSqlParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(FlinkCdcSqlParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(FlinkCdcSqlParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(FlinkCdcSqlParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(FlinkCdcSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(FlinkCdcSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(FlinkCdcSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(FlinkCdcSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(FlinkCdcSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(FlinkCdcSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link FlinkCdcSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(FlinkCdcSqlParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link FlinkCdcSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(FlinkCdcSqlParser.BooleanValueContext ctx);
}