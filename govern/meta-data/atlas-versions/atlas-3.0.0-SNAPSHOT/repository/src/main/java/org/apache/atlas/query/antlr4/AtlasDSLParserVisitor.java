package org.apache.atlas.query.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AtlasDSLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AtlasDSLParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(AtlasDSLParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator(AtlasDSLParser.OperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#sortOrder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSortOrder(AtlasDSLParser.SortOrderContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#valueArray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueArray(AtlasDSLParser.ValueArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(AtlasDSLParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#limitClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimitClause(AtlasDSLParser.LimitClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#offsetClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffsetClause(AtlasDSLParser.OffsetClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#atomE}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomE(AtlasDSLParser.AtomEContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#multiERight}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiERight(AtlasDSLParser.MultiERightContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#multiE}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiE(AtlasDSLParser.MultiEContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#arithERight}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithERight(AtlasDSLParser.ArithERightContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#arithE}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithE(AtlasDSLParser.ArithEContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#comparisonClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonClause(AtlasDSLParser.ComparisonClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#isClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsClause(AtlasDSLParser.IsClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#hasTermClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHasTermClause(AtlasDSLParser.HasTermClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#hasClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHasClause(AtlasDSLParser.HasClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#countClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCountClause(AtlasDSLParser.CountClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#maxClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaxClause(AtlasDSLParser.MaxClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#minClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinClause(AtlasDSLParser.MinClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#sumClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSumClause(AtlasDSLParser.SumClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#exprRight}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprRight(AtlasDSLParser.ExprRightContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#compE}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompE(AtlasDSLParser.CompEContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(AtlasDSLParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#limitOffset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimitOffset(AtlasDSLParser.LimitOffsetContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#selectExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectExpression(AtlasDSLParser.SelectExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#selectExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectExpr(AtlasDSLParser.SelectExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#aliasExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAliasExpr(AtlasDSLParser.AliasExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#orderByExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderByExpr(AtlasDSLParser.OrderByExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#fromSrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromSrc(AtlasDSLParser.FromSrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#whereClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhereClause(AtlasDSLParser.WhereClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#fromExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromExpression(AtlasDSLParser.FromExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#fromClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromClause(AtlasDSLParser.FromClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(AtlasDSLParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#singleQrySrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleQrySrc(AtlasDSLParser.SingleQrySrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#groupByExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupByExpression(AtlasDSLParser.GroupByExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#commaDelimitedQueries}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommaDelimitedQueries(AtlasDSLParser.CommaDelimitedQueriesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#spaceDelimitedQueries}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpaceDelimitedQueries(AtlasDSLParser.SpaceDelimitedQueriesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#querySrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuerySrc(AtlasDSLParser.QuerySrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link AtlasDSLParser#query}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery(AtlasDSLParser.QueryContext ctx);
}