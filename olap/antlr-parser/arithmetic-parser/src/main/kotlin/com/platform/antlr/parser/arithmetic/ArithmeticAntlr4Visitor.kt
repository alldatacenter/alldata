package com.platform.antlr.parser.arithmetic

import com.platform.antlr.parser.common.SQLParserException
import com.platform.antlr.parser.common.relational.Statement
import com.platform.antlr.parser.arithmetic.ArithmeticData
import com.platform.antlr.parser.arithmetic.antlr4.ArithmeticBaseVisitor
import com.platform.antlr.parser.arithmetic.antlr4.ArithmeticParser
import org.antlr.v4.runtime.tree.ParseTree
import org.apache.commons.lang3.StringUtils

/**
 * Created by libinsong on 2020/7/28 9:49 上午
 */
class ArithmeticAntlr4Visitor(val bracketEnbled: Boolean): ArithmeticBaseVisitor<Statement>() {

    private var statement: Statement? = null

    private val arithmetic = ArithmeticData()

    override fun visit(tree: ParseTree): Statement? {
        super.visit(tree)

        if (statement == null) {
            throw SQLParserException("不支持的表达式")
        }

        return statement;
    }

    override fun visitExpression(ctx: ArithmeticParser.ExpressionContext): Statement? {
        statement = arithmetic
        return super.visitExpression(ctx)
    }

    override fun visitIdentifier(ctx: ArithmeticParser.IdentifierContext): Statement? {
        val name = ctx.text
        if (!arithmetic.functions.contains(name)) {
            if (bracketEnbled) {
                arithmetic.variables.add(StringUtils.substringBetween(name, "[", "]"))
            } else {
                arithmetic.variables.add(name)
            }
        }
        return super.visitIdentifier(ctx)
    }

    override fun visitFunctionName(ctx: ArithmeticParser.FunctionNameContext): Statement? {
        val name = ctx.text
        arithmetic.functions.add(name)
        return super.visitFunctionName(ctx)
    }
}