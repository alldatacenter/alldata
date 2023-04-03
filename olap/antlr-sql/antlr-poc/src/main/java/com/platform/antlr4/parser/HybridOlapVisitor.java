// Generated from java-escape by ANTLR 4.11.1

    package com.platform.antlr4.parser;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link HybridOlapParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface HybridOlapVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link HybridOlapParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(HybridOlapParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Add}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd(HybridOlapParser.AddContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Div}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiv(HybridOlapParser.DivContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Min}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMin(HybridOlapParser.MinContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Mul}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMul(HybridOlapParser.MulContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Int}
	 * labeled alternative in {@link HybridOlapParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt(HybridOlapParser.IntContext ctx);
}