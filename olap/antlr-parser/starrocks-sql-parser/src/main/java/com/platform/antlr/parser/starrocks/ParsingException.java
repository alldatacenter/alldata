package com.platform.antlr.parser.starrocks;

import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

public class ParsingException extends RuntimeException {

    private final String detailMsg;

    private final NodePosition pos;

    public ParsingException(String detailMsg, NodePosition pos) {
        super(detailMsg);
        this.detailMsg = detailMsg;
        this.pos = pos;
    }

    // error message should contain position info. This method will be removed in the future.
    public ParsingException(String formatString, Object... args) {
        this(format(formatString, args), NodePosition.ZERO);
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder("Getting syntax error");
        if (pos == null || pos.isZero()) {
            // no position info. do nothing.

        } else if (pos.getLine() == pos.getEndLine() && pos.getCol() == pos.getEndCol()) {
            builder.append(" ");
            builder.append(format("at line %s, column %s", pos.getLine(), pos.getCol()));
        } else {
            builder.append(" ");
            builder.append(format("from line %s, column %s to line %s, column %s",
                    pos.getLine(), pos.getCol(), pos.getEndLine(), pos.getEndCol()));
        }

        if (StringUtils.isNotEmpty(detailMsg)) {
            builder.append(". Detail message: ");
            builder.append(detailMsg);
            builder.append(".");
        }
        return builder.toString();
    }
}
