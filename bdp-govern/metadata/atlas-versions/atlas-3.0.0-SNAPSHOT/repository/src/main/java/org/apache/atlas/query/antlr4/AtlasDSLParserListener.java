package org.apache.atlas.query.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AtlasDSLParser}.
 */
public interface AtlasDSLParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(AtlasDSLParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(AtlasDSLParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#operator}.
	 * @param ctx the parse tree
	 */
	void enterOperator(AtlasDSLParser.OperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#operator}.
	 * @param ctx the parse tree
	 */
	void exitOperator(AtlasDSLParser.OperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#sortOrder}.
	 * @param ctx the parse tree
	 */
	void enterSortOrder(AtlasDSLParser.SortOrderContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#sortOrder}.
	 * @param ctx the parse tree
	 */
	void exitSortOrder(AtlasDSLParser.SortOrderContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#valueArray}.
	 * @param ctx the parse tree
	 */
	void enterValueArray(AtlasDSLParser.ValueArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#valueArray}.
	 * @param ctx the parse tree
	 */
	void exitValueArray(AtlasDSLParser.ValueArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(AtlasDSLParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(AtlasDSLParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void enterLimitClause(AtlasDSLParser.LimitClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void exitLimitClause(AtlasDSLParser.LimitClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#offsetClause}.
	 * @param ctx the parse tree
	 */
	void enterOffsetClause(AtlasDSLParser.OffsetClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#offsetClause}.
	 * @param ctx the parse tree
	 */
	void exitOffsetClause(AtlasDSLParser.OffsetClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#atomE}.
	 * @param ctx the parse tree
	 */
	void enterAtomE(AtlasDSLParser.AtomEContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#atomE}.
	 * @param ctx the parse tree
	 */
	void exitAtomE(AtlasDSLParser.AtomEContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#multiERight}.
	 * @param ctx the parse tree
	 */
	void enterMultiERight(AtlasDSLParser.MultiERightContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#multiERight}.
	 * @param ctx the parse tree
	 */
	void exitMultiERight(AtlasDSLParser.MultiERightContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#multiE}.
	 * @param ctx the parse tree
	 */
	void enterMultiE(AtlasDSLParser.MultiEContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#multiE}.
	 * @param ctx the parse tree
	 */
	void exitMultiE(AtlasDSLParser.MultiEContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#arithERight}.
	 * @param ctx the parse tree
	 */
	void enterArithERight(AtlasDSLParser.ArithERightContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#arithERight}.
	 * @param ctx the parse tree
	 */
	void exitArithERight(AtlasDSLParser.ArithERightContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#arithE}.
	 * @param ctx the parse tree
	 */
	void enterArithE(AtlasDSLParser.ArithEContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#arithE}.
	 * @param ctx the parse tree
	 */
	void exitArithE(AtlasDSLParser.ArithEContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#comparisonClause}.
	 * @param ctx the parse tree
	 */
	void enterComparisonClause(AtlasDSLParser.ComparisonClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#comparisonClause}.
	 * @param ctx the parse tree
	 */
	void exitComparisonClause(AtlasDSLParser.ComparisonClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#isClause}.
	 * @param ctx the parse tree
	 */
	void enterIsClause(AtlasDSLParser.IsClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#isClause}.
	 * @param ctx the parse tree
	 */
	void exitIsClause(AtlasDSLParser.IsClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#hasTermClause}.
	 * @param ctx the parse tree
	 */
	void enterHasTermClause(AtlasDSLParser.HasTermClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#hasTermClause}.
	 * @param ctx the parse tree
	 */
	void exitHasTermClause(AtlasDSLParser.HasTermClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#hasClause}.
	 * @param ctx the parse tree
	 */
	void enterHasClause(AtlasDSLParser.HasClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#hasClause}.
	 * @param ctx the parse tree
	 */
	void exitHasClause(AtlasDSLParser.HasClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#countClause}.
	 * @param ctx the parse tree
	 */
	void enterCountClause(AtlasDSLParser.CountClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#countClause}.
	 * @param ctx the parse tree
	 */
	void exitCountClause(AtlasDSLParser.CountClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#maxClause}.
	 * @param ctx the parse tree
	 */
	void enterMaxClause(AtlasDSLParser.MaxClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#maxClause}.
	 * @param ctx the parse tree
	 */
	void exitMaxClause(AtlasDSLParser.MaxClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#minClause}.
	 * @param ctx the parse tree
	 */
	void enterMinClause(AtlasDSLParser.MinClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#minClause}.
	 * @param ctx the parse tree
	 */
	void exitMinClause(AtlasDSLParser.MinClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#sumClause}.
	 * @param ctx the parse tree
	 */
	void enterSumClause(AtlasDSLParser.SumClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#sumClause}.
	 * @param ctx the parse tree
	 */
	void exitSumClause(AtlasDSLParser.SumClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#exprRight}.
	 * @param ctx the parse tree
	 */
	void enterExprRight(AtlasDSLParser.ExprRightContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#exprRight}.
	 * @param ctx the parse tree
	 */
	void exitExprRight(AtlasDSLParser.ExprRightContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#compE}.
	 * @param ctx the parse tree
	 */
	void enterCompE(AtlasDSLParser.CompEContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#compE}.
	 * @param ctx the parse tree
	 */
	void exitCompE(AtlasDSLParser.CompEContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(AtlasDSLParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(AtlasDSLParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#limitOffset}.
	 * @param ctx the parse tree
	 */
	void enterLimitOffset(AtlasDSLParser.LimitOffsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#limitOffset}.
	 * @param ctx the parse tree
	 */
	void exitLimitOffset(AtlasDSLParser.LimitOffsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#selectExpression}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpression(AtlasDSLParser.SelectExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#selectExpression}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpression(AtlasDSLParser.SelectExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpr(AtlasDSLParser.SelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpr(AtlasDSLParser.SelectExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#aliasExpr}.
	 * @param ctx the parse tree
	 */
	void enterAliasExpr(AtlasDSLParser.AliasExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#aliasExpr}.
	 * @param ctx the parse tree
	 */
	void exitAliasExpr(AtlasDSLParser.AliasExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#orderByExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrderByExpr(AtlasDSLParser.OrderByExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#orderByExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrderByExpr(AtlasDSLParser.OrderByExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#fromSrc}.
	 * @param ctx the parse tree
	 */
	void enterFromSrc(AtlasDSLParser.FromSrcContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#fromSrc}.
	 * @param ctx the parse tree
	 */
	void exitFromSrc(AtlasDSLParser.FromSrcContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void enterWhereClause(AtlasDSLParser.WhereClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#whereClause}.
	 * @param ctx the parse tree
	 */
	void exitWhereClause(AtlasDSLParser.WhereClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#fromExpression}.
	 * @param ctx the parse tree
	 */
	void enterFromExpression(AtlasDSLParser.FromExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#fromExpression}.
	 * @param ctx the parse tree
	 */
	void exitFromExpression(AtlasDSLParser.FromExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(AtlasDSLParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(AtlasDSLParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(AtlasDSLParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(AtlasDSLParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#singleQrySrc}.
	 * @param ctx the parse tree
	 */
	void enterSingleQrySrc(AtlasDSLParser.SingleQrySrcContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#singleQrySrc}.
	 * @param ctx the parse tree
	 */
	void exitSingleQrySrc(AtlasDSLParser.SingleQrySrcContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#groupByExpression}.
	 * @param ctx the parse tree
	 */
	void enterGroupByExpression(AtlasDSLParser.GroupByExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#groupByExpression}.
	 * @param ctx the parse tree
	 */
	void exitGroupByExpression(AtlasDSLParser.GroupByExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#commaDelimitedQueries}.
	 * @param ctx the parse tree
	 */
	void enterCommaDelimitedQueries(AtlasDSLParser.CommaDelimitedQueriesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#commaDelimitedQueries}.
	 * @param ctx the parse tree
	 */
	void exitCommaDelimitedQueries(AtlasDSLParser.CommaDelimitedQueriesContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#spaceDelimitedQueries}.
	 * @param ctx the parse tree
	 */
	void enterSpaceDelimitedQueries(AtlasDSLParser.SpaceDelimitedQueriesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#spaceDelimitedQueries}.
	 * @param ctx the parse tree
	 */
	void exitSpaceDelimitedQueries(AtlasDSLParser.SpaceDelimitedQueriesContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#querySrc}.
	 * @param ctx the parse tree
	 */
	void enterQuerySrc(AtlasDSLParser.QuerySrcContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#querySrc}.
	 * @param ctx the parse tree
	 */
	void exitQuerySrc(AtlasDSLParser.QuerySrcContext ctx);
	/**
	 * Enter a parse tree produced by {@link AtlasDSLParser#query}.
	 * @param ctx the parse tree
	 */
	void enterQuery(AtlasDSLParser.QueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link AtlasDSLParser#query}.
	 * @param ctx the parse tree
	 */
	void exitQuery(AtlasDSLParser.QueryContext ctx);
}