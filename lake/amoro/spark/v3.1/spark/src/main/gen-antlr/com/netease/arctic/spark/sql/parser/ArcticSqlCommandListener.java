package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlCommand.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ArcticSqlCommandParser}.
 */
public interface ArcticSqlCommandListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void enterArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#arcticCommand}.
	 * @param ctx the parse tree
	 */
	void exitArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code migrateStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void enterMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code migrateStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#arcticStatement}.
	 * @param ctx the parse tree
	 */
	void exitMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterMultipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitMultipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterErrorCapturingIdentifier(ArcticSqlCommandParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitErrorCapturingIdentifier(ArcticSqlCommandParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterErrorIdent(ArcticSqlCommandParser.ErrorIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitErrorIdent(ArcticSqlCommandParser.ErrorIdentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void enterRealIdent(ArcticSqlCommandParser.RealIdentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 */
	void exitRealIdent(ArcticSqlCommandParser.RealIdentContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(ArcticSqlCommandParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(ArcticSqlCommandParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedIdentifier(ArcticSqlCommandParser.UnquotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedIdentifier(ArcticSqlCommandParser.UnquotedIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifierAlternative(ArcticSqlCommandParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifierAlternative(ArcticSqlCommandParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ArcticSqlCommandParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterQuotedIdentifier(ArcticSqlCommandParser.QuotedIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link ArcticSqlCommandParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitQuotedIdentifier(ArcticSqlCommandParser.QuotedIdentifierContext ctx);
}
