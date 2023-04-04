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

package datart.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import datart.core.base.PageInfo;
import datart.core.base.consts.Const;
import datart.core.base.consts.ValueType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.common.RequestContext;
import datart.core.data.provider.*;
import datart.core.entity.RelSubjectColumns;
import datart.core.entity.Source;
import datart.core.entity.View;
import datart.core.mappers.ext.RelSubjectColumnsMapperExt;
import datart.security.util.AESUtil;
import datart.server.base.dto.VariableValue;
import datart.server.base.params.TestExecuteParam;
import datart.server.base.params.ViewExecuteParam;
import datart.server.service.BaseService;
import datart.server.service.DataProviderService;
import datart.server.service.VariableService;
import datart.server.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataProviderServiceImpl extends BaseService implements DataProviderService {

    // build in variables
    private static final String VARIABLE_NAME = "DATART_USER_NAME";

    private static final String VARIABLE_USERNAME = "DATART_USER_USERNAME";

    private static final String VARIABLE_EMAIL = "DATART_USER_EMAIL";

    private static final String VARIABLE_ID = "DATART_USER_ID";

    private static final String SERVER_AGGREGATE = "serverAggregate";

    private ObjectMapper objectMapper;

    private final DataProviderManager dataProviderManager;

    private final RelSubjectColumnsMapperExt rscMapper;

    private final VariableService variableService;

    private final ViewService viewService;

    public DataProviderServiceImpl(DataProviderManager dataProviderManager,
                                   RelSubjectColumnsMapperExt rscMapper,
                                   VariableService variableService,
                                   ViewService viewService) {
        this.dataProviderManager = dataProviderManager;
        this.rscMapper = rscMapper;
        this.variableService = variableService;
        this.viewService = viewService;
    }

    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<DataProviderInfo> getSupportedDataProviders() {
        return dataProviderManager.getSupportedDataProviders();
    }

    @Override
    public DataProviderConfigTemplate getSourceConfigTemplate(String type) throws IOException {
        return dataProviderManager.getSourceConfigTemplate(type);
    }

    @Override
    public Object testConnection(DataProviderSource source) throws Exception {
        Map<String, Object> properties = source.getProperties();
        if (!CollectionUtils.isEmpty(properties)) {
            for (String key : properties.keySet()) {
                Object val = properties.get(key);
                if (val instanceof String) {
                    properties.put(key, decryptValue(val.toString()));
                }
            }
        }
        return dataProviderManager.testConnection(source);
    }

    @Override
    public Set<String> readAllDatabases(String sourceId) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readAllDatabases(parseDataProviderConfig(source));
    }

    @Override
    public Set<String> readTables(String sourceId, String database) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readTables(parseDataProviderConfig(source), database);
    }

    @Override
    public Set<Column> readTableColumns(String sourceId, String database, String table) throws SQLException {
        Source source = retrieve(sourceId, Source.class, false);
        return dataProviderManager.readTableColumns(parseDataProviderConfig(source), database, table);
    }

    @Override
    public DataProviderSource parseDataProviderConfig(Source source) {
        DataProviderSource providerSource = new DataProviderSource();
        try {
            providerSource.setSourceId(source.getId());
            providerSource.setType(source.getType());
            providerSource.setName(source.getName());
            Map<String, Object> properties = new HashMap<>(16);
            if (StringUtils.isNotBlank(source.getConfig())) {
                properties = objectMapper.readValue(source.getConfig(), HashMap.class);
            }
            // decrypt values
            for (String key : properties.keySet()) {
                Object val = properties.get(key);
                if (val instanceof String) {
                    String dq = decryptValue(val.toString());
                    properties.put(key, dq);
                }
            }
            providerSource.setProperties(properties);
        } catch (Exception e) {
            Exceptions.tr(BaseException.class, "message.provider.config.error");
        }
        return providerSource;
    }


    /**
     * 测试执行。
     * : 权限变量不生效。
     * : 系统变量不生效。
     * : 查询变量使用默认值。
     *
     * @return 执行结果
     */
    @Override
    public Dataframe testExecute(TestExecuteParam testExecuteParam) throws Exception {
        Source source = retrieve(testExecuteParam.getSourceId(), Source.class, true);
        List<ScriptVariable> variables = getOrgVariables(source.getOrgId());
        if (!CollectionUtils.isEmpty(testExecuteParam.getVariables())) {
            variables.addAll(testExecuteParam.getVariables());
        }
        for (ScriptVariable variable : variables) {
            if (variable.isExpression()) {
                variable.setValueType(ValueType.FRAGMENT);
            }
        }
        if (securityManager.isOrgOwner(source.getOrgId())) {
            disablePermissionVariables(variables);
        }
        QueryScript queryScript = QueryScript.builder()
                .test(true)
                .sourceId(source.getId())
                .script(testExecuteParam.getScript())
                .scriptType(testExecuteParam.getScriptType())
                .variables(variables)
                .build();
        DataProviderSource providerSource = parseDataProviderConfig(source);

        ExecuteParam executeParam = ExecuteParam
                .builder()
                .pageInfo(PageInfo.builder().pageNo(1).pageSize(testExecuteParam.getSize()).countTotal(false).build())
                .includeColumns(Collections.singleton(SelectColumn.of(null, "*")))
                .columns(testExecuteParam.getColumns())
                .serverAggregate((boolean) providerSource.getProperties().getOrDefault(SERVER_AGGREGATE, false))
                .cacheEnable(false)
                .build();
        return dataProviderManager.execute(providerSource, queryScript, executeParam);
    }

    @Override
    public Dataframe execute(ViewExecuteParam viewExecuteParam) throws Exception {
        return execute(viewExecuteParam, true);
    }

    @Override
    public Dataframe execute(ViewExecuteParam viewExecuteParam, boolean checkViewPermission) throws Exception {
        if (viewExecuteParam.isEmpty()) {
            return Dataframe.empty();
        }

        //datasource and view
        View view = retrieve(viewExecuteParam.getViewId(), View.class, checkViewPermission);
        Source source = retrieve(view.getSourceId(), Source.class, false);
        DataProviderSource providerSource = parseDataProviderConfig(source);

        boolean scriptPermission = true;
        try {
            viewService.requirePermission(view, Const.MANAGE);
        } catch (Exception e) {
            scriptPermission = false;
        }
        RequestContext.setScriptPermission(scriptPermission);

        //permission and variables
        Set<SelectColumn> columns = parseColumnPermission(view);
        List<ScriptVariable> variables = parseVariables(view, viewExecuteParam);

        if (securityManager.isOrgOwner(view.getOrgId())) {
            disablePermissionVariables(variables);
        }

        QueryScript queryScript = QueryScript.builder()
                .test(false)
                .sourceId(source.getId())
                .script(view.getScript())
                .scriptType(view.getType() == null ? ScriptType.SQL : ScriptType.valueOf(view.getType()))
                .variables(variables)
                .schema(parseSchema(view.getModel()))
                .build();

        if (viewExecuteParam.getPageInfo().getPageNo() < 1) {
            viewExecuteParam.getPageInfo().setPageNo(1);
        }

        viewExecuteParam.getPageInfo().setPageSize(Math.min(viewExecuteParam.getPageInfo().getPageSize(), Integer.MAX_VALUE));

        ExecuteParam queryParam = ExecuteParam.builder()
                .columns(viewExecuteParam.getColumns())
                .keywords(viewExecuteParam.getKeywords())
                .functionColumns(viewExecuteParam.getFunctionColumns())
                .aggregators(viewExecuteParam.getAggregators())
                .filters(viewExecuteParam.getFilters())
                .groups(viewExecuteParam.getGroups())
                .orders(viewExecuteParam.getOrders())
                .pageInfo(viewExecuteParam.getPageInfo())
                .includeColumns(columns)
                .concurrencyOptimize(viewExecuteParam.isConcurrencyControl())
                .serverAggregate((boolean) providerSource.getProperties().getOrDefault(SERVER_AGGREGATE, false))
                .cacheEnable(viewExecuteParam.isCache())
                .cacheExpires(viewExecuteParam.getCacheExpires())
                .build();

        Dataframe dataframe = dataProviderManager.execute(providerSource, queryScript, queryParam);

        if (!viewExecuteParam.isScript() || !scriptPermission) {
            dataframe.setScript(null);
        }
        return dataframe;
    }

    @Override
    public Set<StdSqlOperator> supportedStdFunctions(String sourceId) {

        Source source = retrieve(sourceId, Source.class, false);

        DataProviderSource dataProviderSource = parseDataProviderConfig(source);

        return dataProviderManager.supportedStdFunctions(dataProviderSource);
    }

    @Override
    public boolean validateFunction(String sourceId, String snippet) {
        Source source = retrieve(sourceId, Source.class);
        DataProviderSource dataProviderSource = parseDataProviderConfig(source);
        return dataProviderManager.validateFunction(dataProviderSource, snippet);
    }

    @Override
    public String decryptValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        if (!value.startsWith(Const.ENCRYPT_FLAG)) {
            return value;
        }
        try {
            return AESUtil.decrypt(value.replaceFirst(Const.ENCRYPT_FLAG, ""));
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public void updateSource(Source source) {
        dataProviderManager.updateSource(parseDataProviderConfig(source));
    }

    private void disablePermissionVariables(List<ScriptVariable> variables) {
        for (ScriptVariable variable : variables) {
            if (VariableTypeEnum.PERMISSION.equals(variable.getType())) {
                variable.setDisabled(true);
            }
        }
    }

    private List<ScriptVariable> parseVariables(View view, ViewExecuteParam param) {
        //通用变量
        List<ScriptVariable> variables = new LinkedList<>();
        variables.addAll(getOrgVariables(view.getOrgId()));
        // view自定义变量
        variables.addAll(getViewVariables(view.getId()));
        variables.stream()
                .filter(v -> v.getType().equals(VariableTypeEnum.QUERY))
                .forEach(v -> {
                    //通过参数传值，进行参数替换
                    if (!CollectionUtils.isEmpty(param.getParams()) && param.getParams().containsKey(v.getName())) {
                        v.setValues(param.getParams().get(v.getName()));
                    } else {
                        //没有参数传值，如果是表达式类型作为默认值，在没有给定值的情况下，改变变量类型为表达式
                        if (v.isExpression()) {
                            v.setValueType(ValueType.FRAGMENT);
                        }
                    }
                });
        return variables;
    }

    private List<ScriptVariable> getSysVariables() {
        LinkedList<ScriptVariable> variables = new LinkedList<>();
        variables.add(new ScriptVariable(VARIABLE_NAME,
                VariableTypeEnum.PERMISSION,
                ValueType.STRING,
                getCurrentUser().getName() == null ? Collections.emptySet() : Sets.newHashSet(getCurrentUser().getName()),
                false));
        variables.add(new ScriptVariable(VARIABLE_EMAIL,
                VariableTypeEnum.PERMISSION,
                ValueType.STRING,
                Sets.newHashSet(getCurrentUser().getEmail()),
                false));
        variables.add(new ScriptVariable(VARIABLE_ID,
                VariableTypeEnum.PERMISSION,
                ValueType.STRING,
                Sets.newHashSet(getCurrentUser().getId()),
                false));
        variables.add(new ScriptVariable(VARIABLE_USERNAME,
                VariableTypeEnum.PERMISSION,
                ValueType.STRING,
                Sets.newHashSet(getCurrentUser().getUsername()),
                false));
        return variables;
    }

    private List<ScriptVariable> getViewVariables(String viewId) {
        return variableService.listViewVarValuesByUser(getCurrentUser().getId(), viewId)
                .stream()
                .map(this::convertScriptValue)
                .collect(Collectors.toList());
    }

    private List<ScriptVariable> getOrgVariables(String orgId) {
        // 内置变量
        List<ScriptVariable> variables = new LinkedList<>(getSysVariables());
        // 组织变量
        variables.addAll(variableService.listOrgValue(orgId)
                .stream()
                .map(this::convertScriptValue)
                .collect(Collectors.toList()));
        return variables;
    }

    private ScriptVariable convertScriptValue(VariableValue var) {
        return new ScriptVariable(var.getName(),
                VariableTypeEnum.valueOf(var.getType()),
                ValueType.valueOf(var.getValueType()),
                var.getValues(),
                var.isExpression());
    }

    private Set<SelectColumn> parseColumnPermission(View view) {
        if (securityManager.isOrgOwner(view.getOrgId())) {
            return Collections.singleton(SelectColumn.of(null, "*"));
        }
        try {
            Set<SelectColumn> columns = new HashSet<>();
            List<RelSubjectColumns> relSubjectColumns = rscMapper.listByUser(view.getId(), getCurrentUser().getId());
            for (RelSubjectColumns relSubjectColumn : relSubjectColumns) {
                List<String> cols = (List<String>) objectMapper.readValue(relSubjectColumn.getColumnPermission(), ArrayList.class);
                if (!CollectionUtils.isEmpty(cols)) {
                    for (String col : cols) {
                        if (StringUtils.isNotBlank(col)) {
                            columns.add(SelectColumn.of(null, col.split("\\.")));
                        }
                    }
                }
            }
            return columns;
        } catch (Exception e) {
            Exceptions.e(e);
        }
        return null;
    }

    /**
     * 从 view 中解析配置的schema
     *
     * @param model view.model
     */
    private Map<String, Column> parseSchema(String model) {
        Map<String, Column> schema = new LinkedHashMap<>();
        if (StringUtils.isBlank(model)) {
            return schema;
        }

        JSONObject jsonObject = JSON.parseObject(model, Feature.OrderedField);
        try {
            if (jsonObject.containsKey("columns")) {
                jsonObject = jsonObject.getJSONObject("columns");
                for (String key : jsonObject.keySet()) {
                    JSONObject item = jsonObject.getJSONObject(key);
                    String[] names;
                    if (item.get("name") instanceof JSONArray) {
                        if (item.getJSONArray("name").size() == 1) {
                            String nameString = item.getJSONArray("name").getString(0);
                            try {
                                names = JSONObject.parseArray(nameString).toArray(new String[0]);
                            } catch (JSONException e) {
                                names = new String[]{nameString};
                            }
                        } else {
                            names = item.getJSONArray("name").toArray(new String[0]);
                        }
                    } else {
                        names = new String[]{Optional.ofNullable(item.getString("name")).orElse(key)};
                    }
                    Column column = Column.of(ValueType.valueOf(item.getString("type")), names);
                    schema.put(column.columnKey(), column);
                }
            } else if (jsonObject.containsKey("hierarchy")) {
                jsonObject = jsonObject.getJSONObject("hierarchy");
                for (String key : jsonObject.keySet()) {
                    JSONObject item = jsonObject.getJSONObject(key);
                    if (item.containsKey("children")) {
                        JSONArray children = item.getJSONArray("children");
                        if (children != null && children.size() > 0) {
                            for (int i = 0; i < children.size(); i++) {
                                JSONObject child = children.getJSONObject(i);
                                schema.put(child.getString("name"), Column.of(ValueType.valueOf(child.getString("type")), child.getString("name").split("\\.")));
                            }
                        }
                    } else {
                        schema.put(key, Column.of(ValueType.valueOf(item.getString("type")), key.split("\\.")));
                    }
                }
            } else {
                // 兼容1.0.0-beta.1以前的版本
                for (String key : jsonObject.keySet()) {
                    ValueType type = ValueType.valueOf(jsonObject.getJSONObject(key).getString("type"));
                    schema.put(key, Column.of(type, key));
                }
            }
        } catch (Exception e) {
            log.error("view model parse error", e);
        }
        return schema;
    }

}
