// Generated from java-escape by ANTLR 4.11.1

    package com.platform.antlr4.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TestParser}.
 */
public interface TestListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TestParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(TestParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link TestParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(TestParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Add}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAdd(TestParser.AddContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Add}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAdd(TestParser.AddContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Div}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterDiv(TestParser.DivContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Div}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitDiv(TestParser.DivContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Min}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMin(TestParser.MinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Min}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMin(TestParser.MinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Mul}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMul(TestParser.MulContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Mul}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMul(TestParser.MulContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Int}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterInt(TestParser.IntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Int}
	 * labeled alternative in {@link TestParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitInt(TestParser.IntContext ctx);
}