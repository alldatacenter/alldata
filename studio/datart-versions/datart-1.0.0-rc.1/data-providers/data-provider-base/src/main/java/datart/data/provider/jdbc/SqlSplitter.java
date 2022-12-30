/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.jdbc;

import java.util.ArrayList;
import java.util.List;


public class SqlSplitter {

    public static final char DEFAULT_DELIMITER = ';';

    public static List<String> splitEscaped(String string, char delimiter) {
        List<Token> tokens = tokenize(string, delimiter);
        return processTokens(string, tokens);
    }

    public static List<String> splitEscaped(String string) {
        return splitEscaped(string, DEFAULT_DELIMITER);
    }

    private static List<String> processTokens(String string, List<Token> tokens) {
        final List<String> splits = new ArrayList<>();
        int begin = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getTokenType() == TokenType.DELIMITER) {
                final int pos = token.getPosition();
                final String substring = string.substring(begin, pos);
                if (!substring.trim().isEmpty()) {
                    splits.add(substring);
                }
                begin = pos + 1;
            }
        }
        if (begin < string.length()) {
            final String substring = string.substring(begin);
            if (!substring.trim().isEmpty()) {
                splits.add(substring);
            }
        }
        return splits;
    }

    private static List<Token> tokenize(String string, char delimiter) {
        final List<Token> tokens = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        for (int cursor = 0; cursor < string.length(); ) {
            final char c = string.charAt(cursor);

            int nextChar = cursor + 1;
            if (c == '\'') {
                nextChar = consumeInQuotes(string, '\'', cursor, builder);
                tokens.add(new Token(TokenType.SINGLE_QUOTED, builder.toString(), cursor));
            } else if (c == '"') {
                nextChar = consumeInQuotes(string, '"', cursor, builder);
                tokens.add(new Token(TokenType.DOUBLE_QUOTED, builder.toString(), cursor));
            } else if (c == '`') {
                nextChar = consumeInQuotes(string, '`', cursor, builder);
                tokens.add(new Token(TokenType.BACK_QUOTED, builder.toString(), cursor));
            } else if (c == delimiter) {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(c), cursor));
            } else if (singleLineComment(c, nextChar, string)) {
                nextChar = consumeSingleLineComment(string, nextChar, builder);
                tokens.add(new Token(TokenType.COMMENT, builder.toString(), cursor));
            } else if (multiLineComment(c, nextChar, string)) {
                nextChar = consumeMultiLineComment(string, nextChar, builder);
                tokens.add(new Token(TokenType.COMMENT, builder.toString(), cursor));
            } else if (!Character.isWhitespace(c)) {
                nextChar = consumeUnquoted(string, delimiter, cursor, builder);
                tokens.add(new Token(TokenType.UNQUOTED, builder.toString(), cursor));
            }
            builder.setLength(0);
            cursor = nextChar;
        }

        return tokens;
    }

    private static boolean singleLineComment(char c, int next, String s) {
        return c == '-' && next < s.length() && s.charAt(next) == '-';
    }

    private static int consumeSingleLineComment(String string, int cursor, StringBuilder builder) {
        for (int i = cursor + 1; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c == '\n' || i == string.length() - 1) {
                return i + 1;
            } else {
                builder.append(c);
            }
        }
        throw new IllegalArgumentException(
                "Could not split string. Comment was not closed properly.");
    }

    private static int consumeMultiLineComment(String string, int cursor, StringBuilder builder) {
        for (int i = cursor + 1; i < string.length() - 1; i++) {
            final char c1 = string.charAt(i);
            final char c2 = string.charAt(i + 1);
            if (c1 == '*' && c2 == '/') {
                return i + 2;
            } else {
                builder.append(c1);
                builder.append(c2);
            }
        }
        throw new IllegalArgumentException(
                "Could not split string. Comment was not closed properly.");
    }

    private static boolean multiLineComment(char c, int next, String s) {
        return c == '/' && next < s.length() && s.charAt(next) == '*';
    }

    private static int consumeInQuotes(
            String string, char quote, int cursor, StringBuilder builder) {
        for (int i = cursor + 1; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == quote) {
                if (i + 1 < string.length() && string.charAt(i + 1) == quote) {
                    builder.append(c);
                    builder.append(string.charAt(i + 1));
                    i += 1;
                } else {
                    return i + 1;
                }
            } else {
                builder.append(c);
            }
        }

        throw new IllegalArgumentException(
                "Could not split string. Quoting was not closed properly.");
    }

    private static int consumeUnquoted(
            String string, char delimiter, int cursor, StringBuilder builder) {
        int i;
        for (i = cursor; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == delimiter || c == '\'' || c == '"' || c == '`') {
                return i;
            }
            if (singleLineComment(c, i + 1, string)) {
                return i;
            }
            if (multiLineComment(c, i + 1, string)) {
                return i;
            }
            builder.append(c);
        }

        return i;
    }

    private enum TokenType {
        COMMENT,
        BACK_QUOTED,
        DOUBLE_QUOTED,
        SINGLE_QUOTED,
        UNQUOTED,
        DELIMITER
    }

    private static class Token {
        private final TokenType tokenType;
        private final String string;
        private final int position;

        private Token(TokenType tokenType, String string, int position) {
            this.tokenType = tokenType;
            this.string = string;
            this.position = position;
        }

        public TokenType getTokenType() {
            return tokenType;
        }

        public String getString() {
            return string;
        }

        public int getPosition() {
            return position;
        }
    }

    private SqlSplitter() {
    }
}
