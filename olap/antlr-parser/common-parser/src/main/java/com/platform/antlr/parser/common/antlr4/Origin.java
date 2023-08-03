package com.platform.antlr.parser.common.antlr4;

public class Origin {
    private int line;
    private int startPosition;

    public Origin(int line, int startPosition) {
        this.line = line;
        this.startPosition = startPosition;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }
}
