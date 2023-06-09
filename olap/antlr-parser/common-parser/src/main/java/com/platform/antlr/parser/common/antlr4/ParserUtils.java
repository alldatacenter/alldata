package com.platform.antlr.parser.common.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ParserUtils {

    public static String getString(Token token) {
        return unescapeSQLString(token.getText());
    }

    public static String getString(TerminalNode token) {
        return unescapeSQLString(token.getText());
    }

    public static int getInt(Token token) {
        return Integer.parseInt(token.getText());
    }

    public static String command(ParserRuleContext ctx) {
        CharStream stream = ctx.getStart().getInputStream();
        return stream.getText(Interval.of(0, stream.size()));
    }

    public static Origin position(Token token) {
        return new Origin(token.getLine(), token.getCharPositionInLine());
    }

    private static void appendEscapedChar(StringBuilder sb, char n) {
        switch (n){
            case '0'    : sb.append("\u0000");
            case '\''   : sb.append("\'");
            case '"'    : sb.append("\"");
            case 'b'    : sb.append("\b");
            case 'n'    : sb.append("\n");
            case 'r'    : sb.append("\r");
            case 't'    : sb.append("\t");
            case 'Z'    : sb.append("\u001A");
            case '\\'   : sb.append("\\");
                // The following 2 lines are exactly what MySQL does TODO: why do we do this?
            case '%'    : sb.append("\\%");
            case '_'    : sb.append("\\_");
            default     : sb.append(n);
        }
    }

    private static String unescapeSQLString(String b) {
        Character enclosure = null;
        StringBuilder sb = new StringBuilder(b.length());

        int i = 0;
        int strLength = b.length();
        while (i < strLength) {
            char currentChar = b.charAt(i);
            if (enclosure == null) {
                if (currentChar == '\'' || currentChar == '\"') {
                    enclosure = currentChar;
                }
            } else if (enclosure == currentChar) {
                enclosure = null;
            } else if (currentChar == '\\') {

                if ((i + 6 < strLength) && b.charAt(i + 1) == 'u') {
                    // \u0000 style character literals.

                    int code = 0;
                    int base = i + 2;
                    for(int h=0; h<4; h++ ) {
                        int digit = Character.digit(b.charAt(h + base), 16);
                        code = (code << 4) + digit;
                    }
                    sb.append((char) code);
                    i += 5;
                } else if (i + 4 < strLength) {
                    // \000 style character literals.

                    char i1 = b.charAt(i + 1);
                    char i2 = b.charAt(i + 2);
                    char i3 = b.charAt(i + 3);

                    if ((i1 >= '0' && i1 <= '1') && (i2 >= '0' && i2 <= '7') && (i3 >= '0' && i3 <= '7')) {
                        char tmp = (char) ((i3 - '0') + ((i2 - '0') << 3) + ((i1 - '0') << 6));
                        sb.append(tmp);
                        i += 3;
                    } else {
                        appendEscapedChar(sb, i1);
                        i += 1;
                    }
                } else if (i + 2 < strLength) {
                    // escaped character literals.
                    char n = b.charAt(i + 1);
                    appendEscapedChar(sb, n);
                    i += 1;
                }
            } else {
                // non-escaped character literals.
                sb.append(currentChar);
            }
            i += 1;
        }
        return sb.toString();
    }
}