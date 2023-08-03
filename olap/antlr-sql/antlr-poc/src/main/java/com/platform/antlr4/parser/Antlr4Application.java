package com.platform.antlr4.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Antlr4Application {
    public static void main(String[] args) {
        CharStream input = CharStreams.fromString("100*2+200");
        TestLexer lexer = new TestLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TestParser parser = new TestParser(tokens);
        TestParser.ExprContext tree = parser.expr();
        TestVisitor tv = new TestVisitor();
        tv.visit(tree);
    }

    static class TestVisitor extends TestBaseVisitor<Void> {
        @Override
        public Void visitAdd(TestParser.AddContext ctx) {
            System.out.println("========= test add");
            System.out.println("first arg: " + ctx.expr(0).getText());
            System.out.println("second arg: " + ctx.expr(1).getText());
            return super.visitAdd(ctx);
        }
    }
}