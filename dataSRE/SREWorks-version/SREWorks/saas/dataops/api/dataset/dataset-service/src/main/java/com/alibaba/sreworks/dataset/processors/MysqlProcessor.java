package com.alibaba.sreworks.dataset.processors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.exception.ModelConfigException;
import com.alibaba.sreworks.dataset.connection.MysqlClient;
import com.alibaba.sreworks.dataset.domain.bo.DataInterfaceSortField;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceGroupField;
import com.alibaba.sreworks.dataset.domain.bo.InterfaceQueryField;
import com.alibaba.sreworks.dataset.domain.primary.InterfaceConfig;
import com.google.common.base.Preconditions;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * ES处理器
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/21 15:12
 */

@Slf4j
@Service
public class MysqlProcessor extends AbstractProcessor {

    @Autowired
    MysqlClient mysqlClient;

    private static String VALUE_WITH_OP_FORMATTER = "${operator}(${dim}) as ${field}";

    private static String VALUE_FORMATTER = "${dim} as ${field}";

    private static String GROUP_FORMATTER = "${dim} as ${field}";

    private static String  SELECT_FORMATTER = " SELECT ${values} ";

    private static String  SELECT_WITH_GROUP_FORMATTER = " SELECT ${groups}, ${values} ";

    private static String FROM_TABLE_FORMATTER = " FROM ${tableName} ";

    private static String WHERE_FORMATTER = " WHERE ${query} ";

    private static String GROUP_BY_FORMATTER = " GROUP BY ${groupFields} ";

    private static String ORDER_BY_FORMATTER = " ORDER BY ${orderFields} ";

    @Override
    public Map<String, Object> getModelData(InterfaceConfig interfaceConfig, Map<String, Object> params) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(mysqlClient.getDataSource(interfaceConfig.getDataSourceId()));

        String sqlTemplate = buildSqlTemplate(interfaceConfig);

        Jinjava jinjava = new Jinjava();
        String sql = jinjava.render(sqlTemplate, params);
        log.info(sql);

        // 需要排序
        if (StringUtils.isNotEmpty(interfaceConfig.getSortFields())) {
            List<DataInterfaceSortField> sortFields = parseSortField(interfaceConfig.getSortFields());
            List<String> conditions = sortFields.stream().map(sortField -> sortField.getFieldName() + " " + sortField.getOrder()).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(conditions)) {
                String orderCondition = new StringSubstitutor(
                        new HashMap<String, String>(){
                            {
                                put("orderFields", String.join(COMMA, conditions));
                            }
                        }).replace(ORDER_BY_FORMATTER);
                sql += orderCondition;
            }
        }
        log.info(sql);

        // 需要分页
        if (interfaceConfig.getPaging()) {
            int pageNum = Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_NUM_KEY, DEFAULT_PAGE_NUM)));
            Preconditions.checkArgument(pageNum > 0, "page num must gt 0");
            int pageSize = Integer.parseInt(String.valueOf(params.getOrDefault(PAGE_SIZE_KEY, DEFAULT_PAGE_SIZE)));

            String pageCondition = String.format(" LIMIT %d, %d", (pageNum - 1) * pageSize, pageSize);
            sql += pageCondition;
        } else {
            // 默认查询前DEFAULT_PAGE_SIZE条
            String pageCondition = String.format(" LIMIT %d, %d", DEFAULT_PAGE_NUM - 1, DEFAULT_PAGE_SIZE);
            sql += pageCondition;
        }
        log.info(sql);

        List<JSONObject> datas = parseResultForInterface(jdbcTemplate.queryForList(sql), interfaceConfig);
        return  buildRetData(datas, params, interfaceConfig.getPaging());
    }

    private String buildSqlTemplate(InterfaceConfig interfaceConfig) throws Exception {
        String sqlTemplateFormatter;
        Map<String, String> params = new HashMap<>();

        String tableName = interfaceConfig.getDataSourceTable();
        if (StringUtils.isEmpty(tableName)) {
            throw new ModelConfigException("mysql数据模型配置错误,表名不允许为空");
        }
        params.put("tableName", tableName);

        // 查询数值字段
        List<InterfaceQueryField> queryFieldList = parseInterfaceQueryField(interfaceConfig.getQueryFields());
        if (CollectionUtils.isEmpty(queryFieldList)) {
            throw new ModelConfigException("mysql数据模型配置错误,数值字段列不允许为空");
        }
        List<String> valueFieldParams = new ArrayList<>();
        for(InterfaceQueryField queryField : queryFieldList) {
            if (StringUtils.isEmpty(queryField.getOperator())) {
                String valueFieldParam = new StringSubstitutor(
                        new HashMap<String, String>(){
                            {
                                put("dim", queryField.getDim());
                                put("field", queryField.getField());
                            }
                        }).replace(VALUE_FORMATTER);
                valueFieldParams.add(valueFieldParam);
            } else {
                String valueFieldParam = new StringSubstitutor(
                        new HashMap<String, String>(){
                            {
                                put("operator", queryField.getOperator());
                                put("dim", queryField.getDim());
                                put("field", queryField.getField());
                            }
                        }).replace(VALUE_WITH_OP_FORMATTER);
                valueFieldParams.add(valueFieldParam);
            }
        }
        params.put("values", String.join(COMMA, valueFieldParams));

        // 分组字段
        List<InterfaceGroupField> groupFieldList = parseInterfaceGroupField(interfaceConfig.getGroupFields());
        String query = interfaceConfig.getQlTemplate();
        if (CollectionUtils.isEmpty(groupFieldList)) {
            sqlTemplateFormatter = SELECT_FORMATTER + FROM_TABLE_FORMATTER;
            if (StringUtils.isNotEmpty(query)) {
                sqlTemplateFormatter += WHERE_FORMATTER;
                params.put("query", query);
            }
        } else {
            sqlTemplateFormatter = SELECT_WITH_GROUP_FORMATTER + FROM_TABLE_FORMATTER;
            if (StringUtils.isNotEmpty(query)) {
                sqlTemplateFormatter += WHERE_FORMATTER;
                params.put("query", query);
            }
            sqlTemplateFormatter += GROUP_BY_FORMATTER;

            List<String> groupFieldParams = new ArrayList<>();
            List<String> groupFieldNames = new ArrayList<>();
            for(InterfaceGroupField groupField : groupFieldList) {
                String groupFieldParam = new StringSubstitutor(
                        new HashMap<String, String>(){
                            {
                                put("dim", groupField.getDim());
                                put("field", groupField.getField());
                            }
                        }).replace(GROUP_FORMATTER);
                groupFieldParams.add(groupFieldParam);
                groupFieldNames.add(groupField.getDim());
            }
            params.put("groups", String.join(COMMA, groupFieldParams));
            params.put("groupFields", String.join(COMMA, groupFieldNames));
        }

        log.info(sqlTemplateFormatter);
        String sqlTemplate = new StringSubstitutor(params).replace(sqlTemplateFormatter);
        log.info(sqlTemplate);

        return sqlTemplate;
    }

    private List<JSONObject> parseResultForInterface(List<Map<String, Object>> results, InterfaceConfig interfaceConfig) {
        List<String> responseFields = parseInterfaceResponseParam(interfaceConfig.getResponseParams());

        List<JSONObject> interfaceResults = new ArrayList<>();

        // 仅返回接口所需字段
        results.forEach(result -> {
            JSONObject respResult = new JSONObject();
            responseFields.forEach(responseField -> respResult.put(responseField, result.get(responseField)));
            interfaceResults.add(respResult);
        });

        return interfaceResults;
    }
}
