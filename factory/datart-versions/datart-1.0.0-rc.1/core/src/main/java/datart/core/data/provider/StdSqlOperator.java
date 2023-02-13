package datart.core.data.provider;

import java.util.concurrent.ConcurrentHashMap;

public enum StdSqlOperator {

    //aggregators
    SUM("SUM"),
    AVG("AVG"),
    MAX("MAX"),
    MIN("MIN"),
    COUNT("COUNT"),
    DISTINCT("DISTINCT"),
    VAR("VAR"),
    STDDEV("STDDEV"),
    MEDIAN("MEDIAN"),
//    PERCENTILE("PERCENTILE"),

    // math
    ABS("ABS"),
    CEILING("CEILING"),
    FLOOR("FLOOR"),
    POWER("POWER"),
    ROUND("ROUND"),
    SQRT("SQRT"),
    EXP("EXP"),
    LOG10("LOG10"),
    LN("LN"),
    MOD("MOD"),
    RAND("RAND"),
    DEGREES("DEGREES"),
    RADIANS("RADIANS"),
    TRUNC("TRUNC"),
    SIGN("SIGN"),
    ACOS("ACOS"),
    ASIN("ASIN"),
    ATAN("ATAN"),
    ATAN2("ATAN2"),
    SIN("SIN"),
    COS("COS"),
    TAN("TAN"),
    COT("COT"),

    // strings
    LENGTH("LENGTH"),
    CONCAT("CONCAT"),
    REPLACE("REPLACE"),
    SUBSTRING("SUBSTRING"),
    LOWER("LOWER"),
    UPPER("UPPER"),
    LTRIM("LTRIM"),
    RTRIM("RTRIM"),
    TRIM("TRIM"),
//    REGEXP_SUBSTR("REGEXP_SUBSTR"),

    // date
    NOW("NOW"),
//    TODAY("TODAY"),
    SECOND("SECOND"),
    MINUTE("MINUTE"),
    HOUR("HOUR"),
    DAY("DAY"),
    WEEK("WEEK"),
    QUARTER("QUARTER"),
    MONTH("MONTH"),
    YEAR("YEAR"),
    DAY_OF_WEEK("DAY_OF_WEEK"),
    DAY_OF_MONTH("DAY_OF_MONTH"),
    DAY_OF_YEAR("DAY_OF_YEAR"),

    // date aggregate
    AGG_DATE_YEAR("AGG_DATE_YEAR"),
    AGG_DATE_QUARTER("AGG_DATE_QUARTER"),
    AGG_DATE_MONTH("AGG_DATE_MONTH"),
    AGG_DATE_WEEK("AGG_DATE_WEEK"),
    AGG_DATE_DAY("AGG_DATE_DAY"),

    // operator
    ADD("+"),
    SUBTRACT("/"),
    MULTIPLY("*"),
    DIVIDE("/"),
    EQUALS("="),
    NOT_EQUALS("!="),
    GREETER_THAN(">"),
    GREETER_THAN_EQ(">="),
    LESS_THAN("<"),
    LESS_THAN_EQ("<="),

    // bool
//    CONTAINS("CONTAINS"),
//    BETWEEN("BETWEEN"),

    //
    IF("IF"),
//    CASE("CASE"),
    COALESCE("COALESCE");
//    AND("AND"),
//    OR("OR"),
//    NOT("NOT");

    private final String symbol;

    StdSqlOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static StdSqlOperator symbolOf(String symbol) {
        return OPERATOR_MAP.get(symbol.toLowerCase());
    }

    private static final ConcurrentHashMap<String, StdSqlOperator> OPERATOR_MAP = new ConcurrentHashMap<>();

    static {
        for (StdSqlOperator value : values()) {
            OPERATOR_MAP.put(value.getSymbol().toLowerCase(), value);
        }
    }
}
