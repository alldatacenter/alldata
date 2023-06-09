package com.platform.antlr.parser.oracle.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public abstract class OracleLexerBase extends Lexer {
    public OracleLexerBase self;

    public OracleLexerBase(CharStream input) {
        super(input);
        self = this;
    }

    protected boolean IsNewlineAtPos(int pos) {
        int la = _input.LA(pos);
        return la == -1 || la == '\n';
    }
}
