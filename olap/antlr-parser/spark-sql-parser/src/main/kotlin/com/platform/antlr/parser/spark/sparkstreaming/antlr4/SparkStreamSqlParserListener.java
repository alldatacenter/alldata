// Generated from java-escape by ANTLR 4.11.1
package com.platform.antlr.parser.spark.sparkstreaming.antlr4;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SparkStreamSqlParser}.
 */
public interface SparkStreamSqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(SparkStreamSqlParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(SparkStreamSqlParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void enterSqlStatements(SparkStreamSqlParser.SqlStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void exitSqlStatements(SparkStreamSqlParser.SqlStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#sqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterSqlStatement(SparkStreamSqlParser.SqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#sqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitSqlStatement(SparkStreamSqlParser.SqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#createStreamTable}.
	 * @param ctx the parse tree
	 */
	void enterCreateStreamTable(SparkStreamSqlParser.CreateStreamTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#createStreamTable}.
	 * @param ctx the parse tree
	 */
	void exitCreateStreamTable(SparkStreamSqlParser.CreateStreamTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void enterInsertStatement(SparkStreamSqlParser.InsertStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void exitInsertStatement(SparkStreamSqlParser.InsertStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetStatement(SparkStreamSqlParser.SetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetStatement(SparkStreamSqlParser.SetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(SparkStreamSqlParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(SparkStreamSqlParser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#setKeyExpr}.
	 * @param ctx the parse tree
	 */
	void enterSetKeyExpr(SparkStreamSqlParser.SetKeyExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#setKeyExpr}.
	 * @param ctx the parse tree
	 */
	void exitSetKeyExpr(SparkStreamSqlParser.SetKeyExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#valueKeyExpr}.
	 * @param ctx the parse tree
	 */
	void enterValueKeyExpr(SparkStreamSqlParser.ValueKeyExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#valueKeyExpr}.
	 * @param ctx the parse tree
	 */
	void exitValueKeyExpr(SparkStreamSqlParser.ValueKeyExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpr(SparkStreamSqlParser.SelectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#selectExpr}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpr(SparkStreamSqlParser.SelectExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(SparkStreamSqlParser.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(SparkStreamSqlParser.WordContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void enterColTypeList(SparkStreamSqlParser.ColTypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#colTypeList}.
	 * @param ctx the parse tree
	 */
	void exitColTypeList(SparkStreamSqlParser.ColTypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void enterColType(SparkStreamSqlParser.ColTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#colType}.
	 * @param ctx the parse tree
	 */
	void exitColType(SparkStreamSqlParser.ColTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDataType(SparkStreamSqlParser.DataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDataType(SparkStreamSqlParser.DataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyList(SparkStreamSqlParser.TablePropertyListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyList}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyList(SparkStreamSqlParser.TablePropertyListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void enterTableProperty(SparkStreamSqlParser.TablePropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#tableProperty}.
	 * @param ctx the parse tree
	 */
	void exitTableProperty(SparkStreamSqlParser.TablePropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyKey(SparkStreamSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyKey}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyKey(SparkStreamSqlParser.TablePropertyKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void enterTablePropertyValue(SparkStreamSqlParser.TablePropertyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#tablePropertyValue}.
	 * @param ctx the parse tree
	 */
	void exitTablePropertyValue(SparkStreamSqlParser.TablePropertyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void enterBooleanValue(SparkStreamSqlParser.BooleanValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#booleanValue}.
	 * @param ctx the parse tree
	 */
	void exitBooleanValue(SparkStreamSqlParser.BooleanValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTableIdentifier(SparkStreamSqlParser.TableIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#tableIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTableIdentifier(SparkStreamSqlParser.TableIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link SparkStreamSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(SparkStreamSqlParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link SparkStreamSqlParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(SparkStreamSqlParser.IdentifierContext ctx);
}