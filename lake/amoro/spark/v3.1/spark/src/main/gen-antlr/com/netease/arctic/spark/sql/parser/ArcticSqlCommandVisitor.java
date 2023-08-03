package com.netease.arctic.spark.sql.parser;// Generated from com/netease/arctic/spark/sql/parser/ArcticSqlCommand.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link ArcticSqlCommandParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface ArcticSqlCommandVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link ArcticSqlCommandParser#arcticCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArcticCommand(ArcticSqlCommandParser.ArcticCommandContext ctx);
	/**
	 * Visit a parse tree produced by the {@code migrateStatement}
	 * labeled alternative in {@link ArcticSqlCommandParser#arcticStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMigrateStatement(ArcticSqlCommandParser.MigrateStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlCommandParser#multipartIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultipartIdentifier(ArcticSqlCommandParser.MultipartIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlCommandParser#errorCapturingIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorCapturingIdentifier(ArcticSqlCommandParser.ErrorCapturingIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code errorIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitErrorIdent(ArcticSqlCommandParser.ErrorIdentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code realIdent}
	 * labeled alternative in {@link ArcticSqlCommandParser#errorCapturingIdentifierExtra}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRealIdent(ArcticSqlCommandParser.RealIdentContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlCommandParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(ArcticSqlCommandParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unquotedIdentifier}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedIdentifier(ArcticSqlCommandParser.UnquotedIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code quotedIdentifierAlternative}
	 * labeled alternative in {@link ArcticSqlCommandParser#strictIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifierAlternative(ArcticSqlCommandParser.QuotedIdentifierAlternativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link ArcticSqlCommandParser#quotedIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedIdentifier(ArcticSqlCommandParser.QuotedIdentifierContext ctx);
}
