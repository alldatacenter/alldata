// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.arithmetic.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArithmeticParser}.
 */
public interface ArithmeticListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#arithmetic}.
	 * @param ctx the parse tree
	 */
	void enterArithmetic(ArithmeticParser.ArithmeticContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#arithmetic}.
	 * @param ctx the parse tree
	 */
	void exitArithmetic(ArithmeticParser.ArithmeticContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(ArithmeticParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(ArithmeticParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalNot(ArithmeticParser.LogicalNotContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalNot}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalNot(ArithmeticParser.LogicalNotContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterPredicated(ArithmeticParser.PredicatedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicated}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitPredicated(ArithmeticParser.PredicatedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalBinary(ArithmeticParser.LogicalBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalBinary}
	 * labeled alternative in {@link ArithmeticParser#booleanExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalBinary(ArithmeticParser.LogicalBinaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(ArithmeticParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(ArithmeticParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterValueExpressionDefault(ArithmeticParser.ValueExpressionDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valueExpressionDefault}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitValueExpressionDefault(ArithmeticParser.ValueExpressionDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparison(ArithmeticParser.ComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code comparison}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparison(ArithmeticParser.ComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticBinary(ArithmeticParser.ArithmeticBinaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticBinary}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticBinary(ArithmeticParser.ArithmeticBinaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticUnary(ArithmeticParser.ArithmeticUnaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arithmeticUnary}
	 * labeled alternative in {@link ArithmeticParser#valueExpression}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticUnary(ArithmeticParser.ArithmeticUnaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCase(ArithmeticParser.SimpleCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleCase}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCase(ArithmeticParser.SimpleCaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantDefault(ArithmeticParser.ConstantDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantDefault}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantDefault(ArithmeticParser.ConstantDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambda(ArithmeticParser.LambdaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lambda}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambda(ArithmeticParser.LambdaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterColumnReference(ArithmeticParser.ColumnReferenceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnReference}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitColumnReference(ArithmeticParser.ColumnReferenceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(ArithmeticParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedExpression}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(ArithmeticParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(ArithmeticParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subscript}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(ArithmeticParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(ArithmeticParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCall}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(ArithmeticParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterSearchedCase(ArithmeticParser.SearchedCaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code searchedCase}
	 * labeled alternative in {@link ArithmeticParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitSearchedCase(ArithmeticParser.SearchedCaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(ArithmeticParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(ArithmeticParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void enterWhenClause(ArithmeticParser.WhenClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#whenClause}.
	 * @param ctx the parse tree
	 */
	void exitWhenClause(ArithmeticParser.WhenClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(ArithmeticParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(ArithmeticParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArithmeticParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArithmeticParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNullLiteral(ArithmeticParser.NullLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNullLiteral(ArithmeticParser.NullLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterNumericLiteral(ArithmeticParser.NumericLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numericLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitNumericLiteral(ArithmeticParser.NumericLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(ArithmeticParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(ArithmeticParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(ArithmeticParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringLiteral}
	 * labeled alternative in {@link ArithmeticParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(ArithmeticParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void enterSetQuantifier(ArithmeticParser.SetQuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#setQuantifier}.
	 * @param ctx the parse tree
	 */
	void exitSetQuantifier(ArithmeticParser.SetQuantifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(ArithmeticParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code integerLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(ArithmeticParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigIntLiteral(ArithmeticParser.BigIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigIntLiteral(ArithmeticParser.BigIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterSmallIntLiteral(ArithmeticParser.SmallIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code smallIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitSmallIntLiteral(ArithmeticParser.SmallIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterTinyIntLiteral(ArithmeticParser.TinyIntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tinyIntLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitTinyIntLiteral(ArithmeticParser.TinyIntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(ArithmeticParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doubleLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(ArithmeticParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void enterBigDecimalLiteral(ArithmeticParser.BigDecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bigDecimalLiteral}
	 * labeled alternative in {@link ArithmeticParser#number}.
	 * @param ctx the parse tree
	 */
	void exitBigDecimalLiteral(ArithmeticParser.BigDecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArithmeticParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(ArithmeticParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArithmeticParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(ArithmeticParser.BooleanValueContext ctx);
}