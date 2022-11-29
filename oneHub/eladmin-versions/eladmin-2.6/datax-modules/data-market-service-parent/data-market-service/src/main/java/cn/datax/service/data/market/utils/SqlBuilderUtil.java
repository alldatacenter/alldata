package cn.datax.service.data.market.utils;

import cn.datax.service.data.market.api.dto.ReqParam;
import cn.datax.service.data.market.api.enums.WhereType;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于动态构造sql语句
 * ${ segment... } 为一个条件代码块
 *
 * String sql = "select * from user where 1=1
 * 		${ and username = :username }
 * 		${ and password = :password }
 * 		${ and age = :age }"
 *
 * 	Map filters = new HashMap();
 * 	filters.put("username", "yuwei");
 * 	filters.put("age", "12");
 * 	filters.put("id", "123");
 *
 * 	SqlFilterResult result = SqlBuilderUtil.applyFilters(sql, filters);
 *
 *  result.getSql()结果
 * 	select * from user where 1=1 and username=:username and age=:age
 *
 *  result.getAcceptedFilters()结果
 * 	{username=yuwei}
 * 	{age=12}
 */
@Slf4j
public class SqlBuilderUtil {

    private SqlBuilderUtil() {}

    private static volatile SqlBuilderUtil instance;

    public static SqlBuilderUtil getInstance() {
        if(instance == null) {
            synchronized (SqlBuilderUtil.class) {
                if(instance == null) {
                    instance = new SqlBuilderUtil();
                }
            }
        }
        return instance;
    }

    /**
     * 空格
     */
    private final String SPACE = " ";
    /**
     * 冒号占位符
     */
    private final String COLON = ":";
    /**
     * 问号占位符
     */
    private final String MARK = "?";
    /**
     * where关键字
     */
    private final String WHERE_SQL = "WHERE";
    /**
     * AND连接符
     */
    private final String WHERE_AND = "AND";
    /**
     * where 1=1条件
     */
    private final String WHERE_INIT = WHERE_SQL + " 1 = 1";
    /**
     * 左括号
     */
    private final String LEFT_BRACKET = "(";
    /**
     * 右括号
     */
    private final String RIGHT_BRACKET = ")";
    /**
     * 百分号%
     */
    private final String PERCENT_SIGN = "%";
    /**
     * 单引号 '
     */
    private final String SINGLE_QUOTE = "'";
    /**
     * 条件代码块标记开始
     */
    public final String MARK_KEY_START = "${";
    /**
     * 条件代码块标记结束
     */
    public final String MARK_KEY_END = "}";

    /**
     * 拼接命名参数sql
     * @param sql
     * @param params
     * @return
     */
    public String buildHql(String sql, List<ReqParam> params){
        Assert.notNull(sql, "SQL must not be null");
        return buildHql(new StringBuffer(sql), params);
    }

    private String buildHql(StringBuffer sql, List<ReqParam> params){
        if(CollUtil.isEmpty(params)){
            return sql.toString();
        }
        sql.append(SPACE).append(WHERE_INIT);
        for (int i = 0; i < params.size(); i++) {
            ReqParam reqParam = params.get(i);
            sql.append(SPACE).append(MARK_KEY_START).append(WHERE_AND).append(SPACE).append(reqParam.getParamName());
            if (WhereType.LIKE.getType() == reqParam.getWhereType()) {
                // LIKE '%' :username '%' ,:username 两边一定要有空格，如果没有空格，是查询不到数据的
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey())
                        .append(SPACE).append(SINGLE_QUOTE).append(PERCENT_SIGN).append(SINGLE_QUOTE).append(SPACE)
                        .append(COLON).append(reqParam.getParamName())
                        .append(SPACE).append(SINGLE_QUOTE).append(PERCENT_SIGN).append(SINGLE_QUOTE).append(MARK_KEY_END);
            } else if(WhereType.LIKE_LEFT.getType() == reqParam.getWhereType()) {
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey())
                        .append(SPACE).append(SINGLE_QUOTE).append(PERCENT_SIGN).append(SINGLE_QUOTE).append(SPACE)
                        .append(COLON).append(reqParam.getParamName()).append(MARK_KEY_END);
            } else if(WhereType.LIKE_RIGHT.getType() == reqParam.getWhereType()) {
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey())
                        .append(SPACE).append(COLON).append(reqParam.getParamName())
                        .append(SPACE).append(SINGLE_QUOTE).append(PERCENT_SIGN).append(SINGLE_QUOTE).append(MARK_KEY_END);
            } else if(WhereType.NULL.getType() == reqParam.getWhereType() || WhereType.NOT_NULL.getType() == reqParam.getWhereType()){
                // is null或is not null不需要参数值
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey()).append(MARK_KEY_END);
            } else if(WhereType.IN.getType() == reqParam.getWhereType()){
                // in (:ids)
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey())
                        .append(SPACE).append(LEFT_BRACKET)
                        .append(COLON).append(reqParam.getParamName())
                        .append(RIGHT_BRACKET).append(MARK_KEY_END);
            } else {
                sql.append(SPACE).append(WhereType.getWhereType(reqParam.getWhereType()).getKey())
                        .append(SPACE).append(COLON).append(reqParam.getParamName()).append(MARK_KEY_END);
            }
        }
        return sql.toString();
    }

    /**
     * 根据入参动态构造sql语句
     * @param sql
     * @param filters
     * @return
     */
    public SqlFilterResult applyFilters(String sql, Map<String, Object> filters){
        Assert.notNull(sql, "SQL must not be null");
        return applyFilters(new StringBuffer(sql), filters);
    }

    private SqlFilterResult applyFilters(StringBuffer sql, Map<String, Object> filters){
        LinkedHashMap<String, Object> acceptedFilters = new LinkedHashMap<>();
        for (int i = 0, end = 0, start = sql.indexOf(MARK_KEY_START); ((start = sql.indexOf(MARK_KEY_START, end)) >= 0); i++) {
            end = sql.indexOf(MARK_KEY_END, start);
            // 封装该条件代码块中的NamedParameterSql
            ParsedSql parsedSql = getSegmentParsedSql(sql, start, end);
            if (CollUtil.isEmpty(parsedSql.getParamNames())){
                throw new IllegalArgumentException("Not key found in segment=" + sql.substring(start, end + MARK_KEY_END.length()));
            }
            // 判断输入参数filters中是否存在查询参数
            if (isAcceptedKeys(filters, parsedSql.getParamNames())) {
                // 动态构造可执行的sql语句，去掉条件代码块两边的${ }标记符
                if (log.isDebugEnabled()) {
                    log.debug("The filter namedParameters=" + parsedSql.getParamNames() + " is accepted on segment=" + sql.substring(start, end + MARK_KEY_END.length()));
                }
                // 下面方法2选1可以获取条件代码块
                // select id, name from user where 1 = 1 and id = :id
//                String segment = sql.substring(start + MARK_KEY_START.length(), end);
                String segment = parsedSql.getOriginalSql();
                // 转换命名参数:为?
                // select id, name from user where 1 = 1 and id = ?
//                String segment = NamedParameterUtil.substituteNamedParams(parsedSql, filters);
                // 获取传参中包含命名参数的数据
                LinkedHashMap<String, Object> linkAcceptedFilters = NamedParameterUtil.buildValueArray(parsedSql, filters);
                acceptedFilters.putAll(linkAcceptedFilters);
                sql.replace(start, end + MARK_KEY_END.length(), segment);
                end = start + segment.length();
            } else {
                // 抛弃该条件代码块
                if (log.isDebugEnabled()) {
                    log.debug("The filter namedParameters=" + parsedSql.getParamNames() + " is removed from the query on segment=" + sql.substring(start, end + MARK_KEY_END.length()));
                }
                sql.replace(start, end + MARK_KEY_END.length(), "");
                end = start;
            }
        }
        return new SqlFilterResult(sql.toString(), acceptedFilters);
    }

    /**
     * 验证入参，并过滤值为空的入参
     */
    private boolean isAcceptedKeys(Map<String, Object> filters, List<String> keys) {
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = getProperty(filters, key);
            if (!isValuePopulated(value, true)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 封装该条件代码块中的NamedParameterSql
     */
    private ParsedSql getSegmentParsedSql(StringBuffer sql, int start, int end) {
        String segment = sql.substring(start + MARK_KEY_START.length(), end);
        ParsedSql parsedSql = NamedParameterUtil.parseSqlStatement(segment);
        return parsedSql;
    }

    /**
     * 获取参数值
     * @param filters
     * @param key
     * @return
     */
    private Object getProperty(Map<String, Object> filters, String key) {
        if (MapUtil.isEmpty(filters))
            return null;
        return filters.get(key);
    }

    /**
     * 验证参数值是否空
     * @param value
     * @param isRemoveEmpty
     * @return
     */
    private boolean isValuePopulated(Object value, boolean isRemoveEmpty) {
        if (value == null) {
            return false;
        }
        if (isRemoveEmpty) {
            return ObjectUtil.isNotEmpty(value);
        } else {
            return true;
        }
    }

    public class SqlFilterResult implements Serializable {

        private static final long serialVersionUID=1L;

        private String sql;

        private Map<String, Object> acceptedFilters;

        public SqlFilterResult(String sql, Map<String, Object> acceptedFilters) {
            this.setSql(sql);
            this.setAcceptedFilters(acceptedFilters);
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Map<String, Object> getAcceptedFilters() {
            return acceptedFilters;
        }

        public void setAcceptedFilters(Map<String, Object> acceptedFilters) {
            this.acceptedFilters = acceptedFilters;
        }

        @Override
        public String toString() {
            return "SqlFilterResult{" +
                    "sql='" + sql + '\'' +
                    ", acceptedFilters=" + acceptedFilters +
                    '}';
        }
    }
}
