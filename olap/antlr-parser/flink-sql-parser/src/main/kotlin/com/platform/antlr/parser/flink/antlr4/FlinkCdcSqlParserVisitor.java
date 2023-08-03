// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.flink.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link FlinkCdcSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface FlinkCdcSqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#singleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleStatement(FlinkCdcSqlParser.SingleStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code beginStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBeginStatement(FlinkCdcSqlParser.BeginStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code endStatement}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndStatement(FlinkCdcSqlParser.EndStatementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createTable}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateTable(FlinkCdcSqlParser.CreateTableContext ctx);
	/**
	 * Visit a parse tree produced by the {@code createDatabase}
	 * labeled alternative in {@link FlinkCdcSqlParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateDatabase(FlinkCdcSqlParser.CreateDatabaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifier(FlinkCdcSqlParser.MultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#identifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierList(FlinkCdcSqlParser.IdentifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#identifierSeq}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierSeq(FlinkCdcSqlParser.IdentifierSeqContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#stringLit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLit(FlinkCdcSqlParser.StringLitContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#commentSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentSpec(FlinkCdcSqlParser.CommentSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#propertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyList(FlinkCdcSqlParser.PropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperty(FlinkCdcSqlParser.PropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#propertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyKey(FlinkCdcSqlParser.PropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#propertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyValue(FlinkCdcSqlParser.PropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#computeColList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputeColList(FlinkCdcSqlParser.ComputeColListContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#computeColDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComputeColDef(FlinkCdcSqlParser.ComputeColDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(FlinkCdcSqlParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpressionDefault(FlinkCdcSqlParser.ValueExpressionDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparison(FlinkCdcSqlParser.ComparisonContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticBinary(FlinkCdcSqlParser.ArithmeticBinaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link FlinkCdcSqlParser#valueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticUnary(FlinkCdcSqlParser.ArithmeticUnaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnReference(FlinkCdcSqlParser.ColumnReferenceContext ctx);
	/**
	 * Visit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDefault(FlinkCdcSqlParser.ConstantDefaultContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cast}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast(FlinkCdcSqlParser.CastContext ctx);
	/**
	 * Visit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link FlinkCdcSqlParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(FlinkCdcSqlParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(FlinkCdcSqlParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#setQuantifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetQuantifier(FlinkCdcSqlParser.SetQuantifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(FlinkCdcSqlParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link FlinkCdcSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(FlinkCdcSqlParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(FlinkCdcSqlParser.QuotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteral(FlinkCdcSqlParser.NullLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parameterLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterLiteral(FlinkCdcSqlParser.ParameterLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code typeConstructor}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeConstructor(FlinkCdcSqlParser.TypeConstructorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericLiteral(FlinkCdcSqlParser.NumericLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(FlinkCdcSqlParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(FlinkCdcSqlParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code legacyDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLegacyDecimalLiteral(FlinkCdcSqlParser.LegacyDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteral(FlinkCdcSqlParser.IntegerLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigIntLiteral(FlinkCdcSqlParser.BigIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSmallIntLiteral(FlinkCdcSqlParser.SmallIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTinyIntLiteral(FlinkCdcSqlParser.TinyIntLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleLiteral(FlinkCdcSqlParser.DoubleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floatLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatLiteral(FlinkCdcSqlParser.FloatLiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link FlinkCdcSqlParser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBigDecimalLiteral(FlinkCdcSqlParser.BigDecimalLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(FlinkCdcSqlParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticOperator(FlinkCdcSqlParser.ArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link FlinkCdcSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(FlinkCdcSqlParser.BooleanValueContext ctx);
}