package com.platform.antlr.parser.starrocks;

import com.platform.antlr.parser.starrocks.antlr4.StarRocksParser;
import com.platform.antlr.parser.starrocks.antlr4.StarRocksParserBaseListener;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public class PostProcessListener extends StarRocksParserBaseListener {

    private final int maxTokensNum;
    private final int maxExprChildCount;

    public PostProcessListener(int maxTokensNum, int maxExprChildCount) {
        this.maxTokensNum = maxTokensNum;
        this.maxExprChildCount = maxExprChildCount;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        Token token = node.getSymbol();
        int index = token.getTokenIndex();
        if (index >= maxTokensNum) {
            throw new ParsingException("Statement exceeds maximum length limit, please consider modify ''parse_tokens_limit'' session variable");
        }
    }

    @Override
    public void exitExpressionList(StarRocksParser.ExpressionListContext ctx) {
        long childCount = ctx.children.stream().filter(child -> child instanceof StarRocksParser.ExpressionContext).count();
        if (childCount > maxExprChildCount) {
            NodePosition pos = new NodePosition(ctx.start, ctx.stop);
            String msg = String.format("The number of exprs are %s exceeded the maximum limit %s, please consider modify " +
                    "''expr_children_limit'' in BE conf", childCount, maxExprChildCount);
            throw new ParsingException(msg, pos);
        }
    }

    @Override
    public void exitExpressionsWithDefault(StarRocksParser.ExpressionsWithDefaultContext ctx) {
        long childCount = ctx.expressionOrDefault().size();
        if (childCount > maxExprChildCount) {
            NodePosition pos = new NodePosition(ctx.start, ctx.stop);
            String msg = String.format("The number of children in expr are %s exceeded the maximum limit %s, please consider modify " +
                    "''expr_children_limit'' in BE conf", childCount, maxExprChildCount);
            throw new ParsingException(msg, pos);
        }
    }

    @Override
    public void exitInsertStatement(StarRocksParser.InsertStatementContext ctx) {
        long childCount = ctx.expressionsWithDefault().size();
        if (childCount > maxExprChildCount) {
            NodePosition pos = new NodePosition(ctx.start, ctx.stop);
            String msg = String.format("The inserted rows are {0} exceeded the maximum limit {1}, please consider modify " +
                    "''expr_children_limit'' in BE conf", childCount, maxExprChildCount);
            throw new ParsingException(msg, pos);
        }
    }
}