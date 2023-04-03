// Generated from java-escape by ANTLR 4.11.1

    package com.platform.antlr4.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link HybridOlapParser}.
 */
public interface HybridOlapListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link HybridOlapParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(HybridOlapParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link HybridOlapParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(HybridOlapParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Add}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAdd(HybridOlapParser.AddContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Add}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAdd(HybridOlapParser.AddContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Div}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterDiv(HybridOlapParser.DivContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Div}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitDiv(HybridOlapParser.DivContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Min}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMin(HybridOlapParser.MinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Min}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMin(HybridOlapParser.MinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Mul}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterMul(HybridOlapParser.MulContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Mul}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitMul(HybridOlapParser.MulContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Int}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterInt(HybridOlapParser.IntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Int}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitInt(HybridOlapParser.IntContext ctx);
}