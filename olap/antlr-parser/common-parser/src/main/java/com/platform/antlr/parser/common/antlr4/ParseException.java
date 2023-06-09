package com.platform.antlr.parser.common.antlr4;

import org.apache.commons.lang3.StringUtils;

public class ParseException extends RuntimeException {
    private String command;
    private String message;
    private Origin start;
    private Origin stop;

    public ParseException(String message, Origin start, Origin stop) {
        this.message = message;
        this.start = start;
        this.stop = stop;
    }

    public ParseException(String command, String message, Origin start, Origin stop) {
        this.command = command;
        this.message = message;
        this.start = start;
        this.stop = stop;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n").append(message);
        if(start != null) {
            builder.append("(line " + start.getLine() + ", pos " + start.getStartPosition() + ")\n");
            if(StringUtils.isNotBlank(command)) {
                String[] lines = command.split("\n");
                builder.append("\n== SQL ==\n");
                for(int i=0; i<start.getLine(); i++) {
                    builder.append(lines[i]).append("\n");
                }

                for(int i=0; i<start.getStartPosition(); i++) {
                    builder.append("-");
                }
                builder.append("^^^\n");

                for(int i=start.getLine(); i<lines.length; i++) {
                    builder.append(lines[i]).append("\n");
                }
            }
        } else {
            builder.append("\n== SQL ==\n").append(command);
        }

        return StringUtils.trim(builder.toString());
    }

    public ParseException withCommand(String cmd) {
        return new ParseException(cmd, message, start, stop);
    }

    public String getCommand() {
        return command;
    }

    public Origin getStart() {
        return start;
    }

    public Origin getStop() {
        return stop;
    }
}