package com.platform.antlr.parser.appjar.antlr4;// Generated from java-escape by ANTLR 4.11.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AppJarParser}.
 */
public interface AppJarParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AppJarParser#rootx}.
	 * @param ctx the parse tree
	 */
	void enterRootx(AppJarParser.RootxContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#rootx}.
	 * @param ctx the parse tree
	 */
	void exitRootx(AppJarParser.RootxContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#jobTasks}.
	 * @param ctx the parse tree
	 */
	void enterJobTasks(AppJarParser.JobTasksContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#jobTasks}.
	 * @param ctx the parse tree
	 */
	void exitJobTasks(AppJarParser.JobTasksContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#jobTask}.
	 * @param ctx the parse tree
	 */
	void enterJobTask(AppJarParser.JobTaskContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#jobTask}.
	 * @param ctx the parse tree
	 */
	void exitJobTask(AppJarParser.JobTaskContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#jobStatement}.
	 * @param ctx the parse tree
	 */
	void enterJobStatement(AppJarParser.JobStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#jobStatement}.
	 * @param ctx the parse tree
	 */
	void exitJobStatement(AppJarParser.JobStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#resourceNameExpr}.
	 * @param ctx the parse tree
	 */
	void enterResourceNameExpr(AppJarParser.ResourceNameExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#resourceNameExpr}.
	 * @param ctx the parse tree
	 */
	void exitResourceNameExpr(AppJarParser.ResourceNameExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#classNameExpr}.
	 * @param ctx the parse tree
	 */
	void enterClassNameExpr(AppJarParser.ClassNameExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#classNameExpr}.
	 * @param ctx the parse tree
	 */
	void exitClassNameExpr(AppJarParser.ClassNameExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#paramsExpr}.
	 * @param ctx the parse tree
	 */
	void enterParamsExpr(AppJarParser.ParamsExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#paramsExpr}.
	 * @param ctx the parse tree
	 */
	void exitParamsExpr(AppJarParser.ParamsExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#paramExpr}.
	 * @param ctx the parse tree
	 */
	void enterParamExpr(AppJarParser.ParamExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#paramExpr}.
	 * @param ctx the parse tree
	 */
	void exitParamExpr(AppJarParser.ParamExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#fileDir}.
	 * @param ctx the parse tree
	 */
	void enterFileDir(AppJarParser.FileDirContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#fileDir}.
	 * @param ctx the parse tree
	 */
	void exitFileDir(AppJarParser.FileDirContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetStatement(AppJarParser.SetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetStatement(AppJarParser.SetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#unsetStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnsetStatement(AppJarParser.UnsetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#unsetStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnsetStatement(AppJarParser.UnsetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#keyExpr}.
	 * @param ctx the parse tree
	 */
	void enterKeyExpr(AppJarParser.KeyExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#keyExpr}.
	 * @param ctx the parse tree
	 */
	void exitKeyExpr(AppJarParser.KeyExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#valueExpr}.
	 * @param ctx the parse tree
	 */
	void enterValueExpr(AppJarParser.ValueExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#valueExpr}.
	 * @param ctx the parse tree
	 */
	void exitValueExpr(AppJarParser.ValueExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(AppJarParser.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(AppJarParser.WordContext ctx);
	/**
	 * Enter a parse tree produced by {@link AppJarParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(AppJarParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AppJarParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(AppJarParser.EmptyStatementContext ctx);
}