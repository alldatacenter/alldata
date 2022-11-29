package cn.datax.service.data.market.mapping.utils;

import cn.datax.common.exception.DataException;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 带参数sql处理工具类
 */
public class NamedParameterUtil {

//    public static void main(String[] args) {
//        String sql = "select * from user where 1 = 1 ${ and id = :id } ${and name = :name}";
//        int start = sql.indexOf("${");
//        int end = sql.indexOf("}", start);
//        String key = sql.substring(start + 2, end);
//        System.out.println(key);
//        Map<String, Object> params = new HashMap<>();
//        params.put("name", "yuwei");
//        params.put("id", "123");
//        params.put("age", 12);
//        ParsedSql parsedSql = NamedParameterUtil.parseSqlStatement(key);
//        System.out.println(parsedSql);
//        String actualSql = NamedParameterUtil.substituteNamedParams(parsedSql, params);
//        Map<String, Object> acceptedFilters = NamedParameterUtil.buildValueArray(parsedSql, params);
//        System.out.println(actualSql);
//        System.out.println(acceptedFilters);
//        SqlBuilderUtil.SqlFilterResult sqlFilterResult = SqlBuilderUtil.getInstance().applyFilters(sql, params);
//        System.out.println(sqlFilterResult.getSql());
//        Object[] array = new Object[] {};
//        array = sqlFilterResult.getAcceptedFilters().values().toArray();
//        Arrays.stream(array).forEach(s -> System.out.println(s));
//    }

    private NamedParameterUtil() {}

    /**
     * 定义特殊字符（增加最后的自定义的'}'）
     */
    private static final char[] PARAMETER_SEPARATORS =
            new char[] {'"', '\'', ':', '&', ',', ';', '(', ')', '|', '=', '+', '-', '*', '%', '/', '\\', '<', '>', '^', '}'};

    /**
     * 对带参数sql的统计式封装，便于后续肢解拼装
     * @param originalSql
     * @return
     */
    public static ParsedSql parseSqlStatement(String originalSql) {
        Assert.notNull(originalSql, "SQL must not be null");
        ParsedSql parsedSql = new ParsedSql(originalSql);
        Set<String> namedParameters = new HashSet();
        char[] sqlchars = originalSql.toCharArray();
        int namedParamCount = 0;
        int unNamedParamCount = 0;
        int totalParamCount = 0;
        int i = 0;
        while (i < sqlchars.length) {
            char statement = sqlchars[i];
            if (statement == ':') {
                int j = i + 1;
                while (j < sqlchars.length && !isSeparatorsChar(sqlchars[j])) {
                    j++;
                }
                if (j - i > 1) {
                    String paramName = originalSql.substring(i + 1, j);
                    if (!namedParameters.contains(paramName)) {
                        namedParameters.add(paramName);
                        namedParamCount++;
                    }
                    parsedSql.addParamNames(paramName, i, j);
                    totalParamCount++;
                }
                i = j - 1;
            } else if (statement == '?') {
                unNamedParamCount++;
                totalParamCount++;
            }
            i++;
        }
        parsedSql.setNamedParamCount(namedParamCount);
        parsedSql.setUnnamedParamCount(unNamedParamCount);
        parsedSql.setTotalParamCount(totalParamCount);
        return parsedSql;
    }

    /**
     * 获得不带参数的sql，即替换参数为？
     * @param parsedSql
     * @param params
     * @return
     */
    public static String substituteNamedParams(ParsedSql parsedSql, Map<String, Object> params){
        String original = parsedSql.getOriginalSql();
        StringBuffer actual = new StringBuffer("");
        int lastIndex = 0;
        List<String> paramNames = parsedSql.getParamNames();
        for (int i = 0; i < paramNames.size(); i++) {
            int[] indexs = parsedSql.getParamIndexs(i);
            int startIndex = indexs[0];
            int endIndex = indexs[1];
            String paramName = paramNames.get(i);
            actual.append(original.substring(lastIndex, startIndex));
            if (params != null && params.containsKey(paramName)) {
                actual.append("?");
            } else{
                actual.append("?");
            }
            lastIndex = endIndex;
        }
        actual.append(original.subSequence(lastIndex, original.length()));
        return actual.toString();
    }

    /**
     * 获得sql所需参数K,V
     * @param parsedSql
     * @param params
     * @return
     */
    public static LinkedHashMap<String, Object> buildValueArray(ParsedSql parsedSql, Map<String, Object> params){
        List<String> paramNames = parsedSql.getParamNames();
        LinkedHashMap<String, Object> acceptedFilters = new LinkedHashMap<>(parsedSql.getTotalParamCount());
        if (parsedSql.getNamedParamCount() > 0 && parsedSql.getUnnamedParamCount() > 0) {
            throw new DataException("parameter方式与？方式不能混合！");
        }
        for (int i = 0; i < paramNames.size(); i++) {
            String keyName = paramNames.get(i);
            if (params.containsKey(keyName)) {
                acceptedFilters.put(keyName, params.get(keyName));
            }
        }
        return acceptedFilters;
    }

    private static boolean isSeparatorsChar(char statement){
        if (Character.isWhitespace(statement)) {
            return true;
        }
        for (int i = 0; i < PARAMETER_SEPARATORS.length; i++) {
            if (statement == PARAMETER_SEPARATORS[i]) {
                return true;
            }
        }
        return false;
    }
}
