package com.platform.antlr4.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Antlr4Application {
    public static void main(String[] args) {
        CharStream input = CharStreams.fromString("100*2+200");
        HybridOlapLexer lexer = new HybridOlapLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HybridOlapParser parser = new HybridOlapParser(tokens);
        HybridOlapParser.ExprContext tree = parser.expr();
        HybridOlapVisitor tv = new HybridOlapVisitor();
        tv.visit(tree);
    }

    static class HybridOlapVisitor extends HybridOlapBaseVisitor<Void> {
        @Override
        public Void visitAdd(HybridOlapParser.AddContext ctx) {
            System.out.println("========= test add");
            System.out.println("first arg: " + ctx.expr(0).getText());
            System.out.println("second arg: " + ctx.expr(1).getText());
            return super.visitAdd(ctx);
        }
    }
}