// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.spark.sparkstreaming.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SparkStreamSqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SparkStreamSqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#root}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoot(SparkStreamSqlParser.RootContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#sqlStatements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlStatements(SparkStreamSqlParser.SqlStatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#sqlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlStatement(SparkStreamSqlParser.SqlStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#createStreamTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreateStreamTable(SparkStreamSqlParser.CreateStreamTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#insertStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsertStatement(SparkStreamSqlParser.InsertStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#setStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetStatement(SparkStreamSqlParser.SetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#emptyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement(SparkStreamSqlParser.EmptyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#setKeyExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetKeyExpr(SparkStreamSqlParser.SetKeyExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#valueKeyExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueKeyExpr(SparkStreamSqlParser.ValueKeyExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#selectExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectExpr(SparkStreamSqlParser.SelectExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#word}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWord(SparkStreamSqlParser.WordContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColTypeList(SparkStreamSqlParser.ColTypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#colType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColType(SparkStreamSqlParser.ColTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#dataType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDataType(SparkStreamSqlParser.DataTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyList(SparkStreamSqlParser.TablePropertyListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableProperty(SparkStreamSqlParser.TablePropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyKey(SparkStreamSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablePropertyValue(SparkStreamSqlParser.TablePropertyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanValue(SparkStreamSqlParser.BooleanValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableIdentifier(SparkStreamSqlParser.TableIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link SparkStreamSqlParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(SparkStreamSqlParser.IdentifierContext ctx);
}