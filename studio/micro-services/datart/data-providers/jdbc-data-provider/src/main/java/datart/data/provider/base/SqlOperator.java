package datart.data.provider.base;

import lombok.Getter;

import java.util.regex.Pattern;

public enum SqlOperator {

    GT(Pattern.compile("[^\\s<]+(\\s*>\\s*){1}[^\\s=]+"), ">", Pattern.compile("\\s>\\s")),

    GTE(Pattern.compile("[^\\s]+(\\s*>\\s*=\\s*){1}[^\\s]+"), ">=", Pattern.compile("\\s*>\\s*=\\s*")),

    LT(Pattern.compile("[^\\s]+(\\s*<\\s*){1}[^\\s=>]+"), "<", Pattern.compile("\\s*<\\s*")),

    LTE(Pattern.compile("[^\\s]+(\\s*<\\s*=\\s*){1}[^\\s]+"), "<=", Pattern.compile("\\s*<\\s*=\\s*")),

    EQ(Pattern.compile("[^\\s=<]+(\\s*=\\s*){1}[^\\s]+"), "=", Pattern.compile("\\s*=\\s*")),

    NEQ(Pattern.compile("[^\\s]+(\\s*<\\s*>\\s*){1}[^\\s]+"), "<>", Pattern.compile("\\s*<\\s*>\\s*")),

    LIKE(Pattern.compile("[^\\s(not)]+(\\s+like\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "like", Pattern.compile("\\s*like\\s*", Pattern.CASE_INSENSITIVE)),

    UNLIKE(Pattern.compile("[^\\s]+(\\s+not\\s+like\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "not like", Pattern.compile("\\s*not\\s*like\\s*", Pattern.CASE_INSENSITIVE)),

    IN(Pattern.compile("[^\\s(not)]+(\\s+in\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "in", Pattern.compile("\\s*in\\s*", Pattern.CASE_INSENSITIVE)),

    NIN(Pattern.compile("[^\\s]+(\\s+not\\s+in\\s+){1}[^\\s]+", Pattern.CASE_INSENSITIVE), "not in", Pattern.compile("\\s*not\\s*in\\s*", Pattern.CASE_INSENSITIVE));

    SqlOperator(Pattern pattern, String operator, Pattern replace) {
        this.pattern = pattern;
        this.operator = operator;
        this.replace = replace;
    }

    @Getter
    private final Pattern pattern;

    @Getter
    private final String operator;

    @Getter
    private final Pattern replace;

}
