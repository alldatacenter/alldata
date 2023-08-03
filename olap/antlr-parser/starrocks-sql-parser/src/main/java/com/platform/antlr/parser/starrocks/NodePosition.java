package com.platform.antlr.parser.starrocks;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.Serializable;

// Used to record element position in the sql. ParserRuleContext records the input start and end token,
// and we can transform their line and col info to NodePosition.
public class NodePosition implements Serializable {

    public static final NodePosition ZERO = new NodePosition(0, 0);

    private static final long serialVersionUID = 5619050719810060066L;

    private final int line;

    private final int col;

    private final int endLine;

    private final int endCol;

    public NodePosition(TerminalNode node) {
        this(node.getSymbol().getLine(), node.getSymbol().getCharPositionInLine());
    }

    public NodePosition(Token token) {
        this(token.getLine(), token.getLine());
    }

    public NodePosition(Token start, Token end) {
        this(start.getLine(), start.getCharPositionInLine(), end.getLine(), end.getCharPositionInLine());
    }

    public NodePosition(int line, int col) {
        this(line, col, line, col);
    }

    public NodePosition(int line, int col, int endLine, int endCol) {
        this.line = line;
        this.col = col;
        this.endLine = endLine;
        this.endCol = endCol;
    }

    public int getLine() {
        return line;
    }

    public int getCol() {
        return col;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndCol() {
        return endCol;
    }

    public boolean isZero() {
        return line == 0 && col == 0 && endLine == 0 && endCol == 0;
    }


}
