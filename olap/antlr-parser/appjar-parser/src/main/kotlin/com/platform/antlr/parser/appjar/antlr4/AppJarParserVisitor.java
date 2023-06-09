package com.platform.antlr.parser.appjar.antlr4;// Generated from java-escape by ANTLR 4.11.1
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AppJarParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AppJarParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AppJarParser#rootx}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRootx(AppJarParser.RootxContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#jobTasks}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJobTasks(AppJarParser.JobTasksContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#jobTask}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJobTask(AppJarParser.JobTaskContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#jobStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJobStatement(AppJarParser.JobStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#resourceNameExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceNameExpr(AppJarParser.ResourceNameExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#classNameExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNameExpr(AppJarParser.ClassNameExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#paramsExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamsExpr(AppJarParser.ParamsExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#paramExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParamExpr(AppJarParser.ParamExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#fileDir}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileDir(AppJarParser.FileDirContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#setStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetStatement(AppJarParser.SetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#unsetStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsetStatement(AppJarParser.UnsetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#keyExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyExpr(AppJarParser.KeyExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#valueExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueExpr(AppJarParser.ValueExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#word}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWord(AppJarParser.WordContext ctx);
	/**
	 * Visit a parse tree produced by {@link AppJarParser#emptyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStatement(AppJarParser.EmptyStatementContext ctx);
}