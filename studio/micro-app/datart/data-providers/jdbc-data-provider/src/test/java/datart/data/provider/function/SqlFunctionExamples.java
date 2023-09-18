package datart.data.provider.function;

import java.util.ArrayList;
import java.util.List;

public class SqlFunctionExamples {

    public static List<String> functionList = new ArrayList<>();

    static {
        init();
    }

    private static void init(){
        functionList.add("SUM(num)");
        functionList.add("MAX(num)");
        functionList.add("MIN(num)");
        functionList.add("AVG(num)");
        functionList.add("distinct id");
        functionList.add("var(num)");
        functionList.add("STDDEV(num)");
        functionList.add("MEDIAN(num)");
        functionList.add("TRIM(str)");
        //functionList.add("TRIM(`name`)");
        functionList.add("LTRIM(str)");
        functionList.add("RTRIM(str)");
        functionList.add("count(*)");
        functionList.add("abs(num)");
        functionList.add("POWER(num)");
        functionList.add("floor(num)");
        functionList.add("ceiling(num)");
        functionList.add("round(num)");
        functionList.add("lower(num)");
        functionList.add("upper(num)");
        functionList.add("LENGTH(string)");
        functionList.add("concat(concat('2',concat('3','6')),'1')");
        functionList.add("REPLACE('teststr', 'str', 'Str')");
        functionList.add("NOW()");
        functionList.add("SECOND(cr_time)");
        functionList.add("MINUTE(cr_time)");
        functionList.add("HOUR(cr_time)");
        functionList.add("QUARTER(cr_time)");
        //functionList.add("DAY(cr_time)");
        functionList.add("WEEK(cr_time)");
        functionList.add("MONTH(cr_time)");
        functionList.add("YEAR(cr_time)");
        functionList.add("DAY_OF_WEEK(cr_time)");
        functionList.add("DAY_OF_MONTH(cr_time)");
        functionList.add("DAY_OF_YEAR(cr_time)");
        functionList.add("CASE WHEN AGE>=18 THEN '成年' ELSE '未成年' end");
        functionList.add("IFNULL(str)");
        functionList.add("IF(500<1000, 5, 10)");
        functionList.add("substr('string',1,3)");
        functionList.add("SQRT(num)");
        functionList.add("EXP(num)");
        functionList.add("LOG10(num)");
        functionList.add("LN(num)");
        functionList.add("MOD(29,3)");
        functionList.add("RAND(10)");
        functionList.add("DEGREES(10)");
        functionList.add("RADIANS(100)");
        functionList.add("TRUNC(12.345,2)");
        functionList.add("TRUNC(12.345,2)");
        functionList.add("SIGN(12.345,2)");
        functionList.add("ACOS(num)");
        functionList.add("ASIN(num)");
        functionList.add("ATAN(num)");
        functionList.add("ATAN2(num)");
        functionList.add("SIN(num)");
        functionList.add("COS(num)");
        functionList.add("TAN(num)");
        functionList.add("COT(num)");
        functionList.add("LENGTH(name)");
        //functionList.add("date_add(date1 ,interval - day(date2) + 1 day)");
    }
}
