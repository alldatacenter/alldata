/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.service.impl;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.config.ConnectorDataSourceMapperConfig;
import org.apache.seatunnel.app.dal.dao.IDatasourceDao;
import org.apache.seatunnel.app.dal.dao.IVirtualTableDao;
import org.apache.seatunnel.app.dal.entity.Datasource;
import org.apache.seatunnel.app.dal.entity.VirtualTable;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.datasource.DatasourceDetailRes;
import org.apache.seatunnel.app.domain.response.datasource.DatasourceRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableFieldRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.permission.constants.SeatunnelFuncPermissionKeyConstant;
import org.apache.seatunnel.app.permission.enums.SeatunnelResourcePermissionModuleEnum;
import org.apache.seatunnel.app.service.IDatasourceService;
import org.apache.seatunnel.app.service.IJobDefinitionService;
import org.apache.seatunnel.app.service.ITableSchemaService;
import org.apache.seatunnel.app.thirdparty.datasource.DataSourceClientFactory;
import org.apache.seatunnel.app.thirdparty.framework.SeaTunnelOptionRuleWrapper;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginInfo;
import org.apache.seatunnel.datasource.plugin.api.DatasourcePluginTypeEnum;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.server.common.CodeGenerateUtils;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DatasourceServiceImpl extends SeatunnelBaseServiceImpl
        implements IDatasourceService, ApplicationContextAware {

    private static final String VIRTUAL_TABLE_DATABASE_NAME = "default";

    @Autowired
    @Qualifier("datasourceDaoImpl") private IDatasourceDao datasourceDao;

    private ApplicationContext applicationContext;

    @Resource private IJobDefinitionService jobDefinitionService;

    @Autowired
    @Qualifier("virtualTableDaoImpl") private IVirtualTableDao virtualTableDao;

    @Autowired private ConnectorDataSourceMapperConfig dataSourceMapperConfig;

    protected static final String DEFAULT_DATASOURCE_PLUGIN_VERSION = "1.0.0";

    @Override
    public String createDatasource(
            Integer userId,
            String datasourceName,
            String pluginName,
            String pluginVersion,
            String description,
            Map<String, String> datasourceConfig)
            throws CodeGenerateUtils.CodeGenerateException {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_CREATE, userId);
        long uuid = CodeGenerateUtils.getInstance().genCode();
        boolean unique = datasourceDao.checkDatasourceNameUnique(datasourceName, 0L);
        if (!unique) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.DATASOURCE_NAME_ALREADY_EXISTS, datasourceName);
        }
        if (MapUtils.isEmpty(datasourceConfig)) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.DATASOURCE_PRAM_NOT_ALLOWED_NULL, "datasourceConfig");
        }
        String datasourceConfigStr = JsonUtils.toJsonString(datasourceConfig);
        Datasource datasource =
                Datasource.builder()
                        .id(uuid)
                        .createUserId(userId)
                        .updateUserId(userId)
                        .datasourceName(datasourceName)
                        .pluginName(pluginName)
                        .pluginVersion(pluginVersion)
                        .description(description)
                        .datasourceConfig(datasourceConfigStr)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
        boolean success = datasourceDao.insertDatasource(datasource);
        if (success) {
            resourcePostHandle(
                    SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                    Collections.singletonList(datasource.getId()),
                    userId);
            return String.valueOf(uuid);
        }
        throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_CREATE_FAILED);
    }

    @Override
    public boolean updateDatasource(
            Integer userId,
            Long datasourceId,
            String datasourceName,
            String description,
            Map<String, String> datasourceConfig) {
        funcAndResourcePermissionCheck(
                SeatunnelFuncPermissionKeyConstant.DATASOURCE_UPDATE,
                SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                Collections.singletonList(datasourceId),
                userId);
        if (datasourceId == null) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.DATASOURCE_PRAM_NOT_ALLOWED_NULL, "datasourceId");
        }
        Datasource datasource = datasourceDao.selectDatasourceById(datasourceId);
        if (datasource == null) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceId.toString());
        }
        if (StringUtils.isNotBlank(datasourceName)) {
            datasource.setDatasourceName(datasourceName);
            boolean unique = datasourceDao.checkDatasourceNameUnique(datasourceName, datasourceId);
            if (!unique) {
                throw new SeatunnelException(
                        SeatunnelErrorEnum.DATASOURCE_NAME_ALREADY_EXISTS, datasourceName);
            }
        }
        datasource.setUpdateUserId(userId);
        datasource.setUpdateTime(new Date());
        datasource.setDescription(description);
        if (MapUtils.isNotEmpty(datasourceConfig)) {
            String configJson = JsonUtils.toJsonString(datasourceConfig);
            datasource.setDatasourceConfig(configJson);
        }
        return datasourceDao.updateDatasourceById(datasource);
    }

    @Override
    public boolean deleteDatasource(Integer userId, Long datasourceId) {
        // check role permission
        funcAndResourcePermissionCheck(
                SeatunnelFuncPermissionKeyConstant.DATASOURCE_DELETE,
                SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                Collections.singletonList(datasourceId),
                userId);
        // check has virtual table has used this datasource
        List<String> virtualDatabaseNames = virtualTableDao.getVirtualDatabaseNames(datasourceId);
        if (!CollectionUtils.isEmpty(virtualDatabaseNames)) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_CAN_NOT_DELETE);
        }
        if (!jobDefinitionService.getJobVersionByDataSourceId(datasourceId).isEmpty()) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_CAN_NOT_DELETE);
        }
        return datasourceDao.deleteDatasourceById(datasourceId);
    }

    @Override
    public boolean testDatasourceConnectionAble(
            Integer userId,
            String pluginName,
            String pluginVersion,
            Map<String, String> datasourceConfig) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_TEST_CONNECT, userId);
        return DataSourceClientFactory.getDataSourceClient()
                .checkDataSourceConnectivity(pluginName, datasourceConfig);
    }

    @Override
    public boolean testDatasourceConnectionAble(Integer userId, Long datasourceId) {
        funcAndResourcePermissionCheck(
                SeatunnelFuncPermissionKeyConstant.DATASOURCE_TEST_CONNECT,
                SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                Collections.singletonList(datasourceId),
                userId);
        Datasource datasource = datasourceDao.selectDatasourceById(datasourceId);
        if (datasource == null) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceId.toString());
        }
        String configJson = datasource.getDatasourceConfig();
        Map<String, String> datasourceConfig =
                JsonUtils.toMap(configJson, String.class, String.class);
        String pluginName = datasource.getPluginName();
        return DataSourceClientFactory.getDataSourceClient()
                .checkDataSourceConnectivity(pluginName, datasourceConfig);
    }

    @Override
    public String queryDatasourceNameById(String datasourceId) {
        long datasourceIdLong = Long.parseLong(datasourceId);
        return datasourceDao.queryDatasourceNameById(datasourceIdLong);
    }

    @Override
    public String getDynamicForm(String pluginName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_DYNAMIC, 0);
        OptionRule optionRule =
                DataSourceClientFactory.getDataSourceClient()
                        .queryDataSourceFieldByName(pluginName);
        // If the plugin doesn't have connector will directly use pluginName
        String connectorForDatasourceName =
                dataSourceMapperConfig
                        .findConnectorForDatasourceName(pluginName)
                        .orElse(pluginName);
        FormStructure testForm =
                SeaTunnelOptionRuleWrapper.wrapper(optionRule, connectorForDatasourceName);
        return JsonUtils.toJsonString(testForm);
    }

    @Override
    public boolean checkDatasourceNameUnique(
            Integer userId, String datasourceName, Long dataSourceId) {
        if (StringUtils.isNotBlank(datasourceName)) {
            return datasourceDao.checkDatasourceNameUnique(datasourceName, dataSourceId);
        }
        return false;
    }

    @Override
    public List<String> queryDatabaseByDatasourceName(String datasourceName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_DATABASES, 0);
        Datasource datasource = datasourceDao.queryDatasourceByName(datasourceName);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceName);
        }
        String pluginName = datasource.getPluginName();
        if (Boolean.FALSE.equals(checkIsSupportVirtualTable(pluginName))) {
            String config = datasource.getDatasourceConfig();
            Map<String, String> datasourceConfig =
                    JsonUtils.toMap(config, String.class, String.class);

            return DataSourceClientFactory.getDataSourceClient()
                    .getDatabases(pluginName, datasourceConfig);
        }
        long dataSourceId = datasource.getId();
        boolean hasVirtualTable = virtualTableDao.checkHasVirtualTable(dataSourceId);
        if (hasVirtualTable) {
            return Collections.singletonList(VIRTUAL_TABLE_DATABASE_NAME);
        }
        return new ArrayList<>();
    }

    private boolean checkIsSupportVirtualTable(String pluginName) {
        return DataSourceClientFactory.getDataSourceClient().listAllDataSources().stream()
                .anyMatch(d -> d.getName().equals(pluginName) && d.getSupportVirtualTables());
    }

    @Override
    public List<String> queryTableNames(
            String datasourceName, String databaseName, String filterName, Integer size) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_TABLE, 0);
        Datasource datasource = datasourceDao.queryDatasourceByName(datasourceName);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceName);
        }
        String config = datasource.getDatasourceConfig();
        Map<String, String> datasourceConfig = JsonUtils.toMap(config, String.class, String.class);
        Map<String, String> options = new HashMap<>();
        options.put("size", size.toString());
        options.put("filterName", filterName);
        String pluginName = datasource.getPluginName();
        if (BooleanUtils.isNotTrue(checkIsSupportVirtualTable(pluginName))) {
            return DataSourceClientFactory.getDataSourceClient()
                    .getTables(pluginName, databaseName, datasourceConfig, options);
        }
        long dataSourceId = datasource.getId();
        return virtualTableDao.getVirtualTableNames(VIRTUAL_TABLE_DATABASE_NAME, dataSourceId);
    }

    @Override
    public List<String> queryTableNames(String datasourceName, String databaseName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_TABLE, 0);
        Datasource datasource = datasourceDao.queryDatasourceByName(datasourceName);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceName);
        }
        String config = datasource.getDatasourceConfig();
        Map<String, String> datasourceConfig = JsonUtils.toMap(config, String.class, String.class);
        Map<String, String> options = new HashMap<>();
        String pluginName = datasource.getPluginName();
        if (BooleanUtils.isNotTrue(checkIsSupportVirtualTable(pluginName))) {
            return DataSourceClientFactory.getDataSourceClient()
                    .getTables(pluginName, databaseName, datasourceConfig, options);
        }
        long dataSourceId = datasource.getId();
        return virtualTableDao.getVirtualTableNames(VIRTUAL_TABLE_DATABASE_NAME, dataSourceId);
    }

    @Override
    public List<TableField> queryTableSchema(
            String datasourceName, String databaseName, String tableName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_TABLE_SCHEMA, 0);
        Datasource datasource = datasourceDao.queryDatasourceByName(datasourceName);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceName);
        }
        String config = datasource.getDatasourceConfig();
        Map<String, String> datasourceConfig = JsonUtils.toMap(config, String.class, String.class);
        String pluginName = datasource.getPluginName();
        ITableSchemaService tableSchemaService =
                (ITableSchemaService) applicationContext.getBean("tableSchemaServiceImpl");
        if (BooleanUtils.isNotTrue(checkIsSupportVirtualTable(pluginName))) {
            List<TableField> tableFields =
                    DataSourceClientFactory.getDataSourceClient()
                            .getTableFields(pluginName, datasourceConfig, databaseName, tableName);

            tableSchemaService.getAddSeaTunnelSchema(tableFields, pluginName);
            return tableFields;
        }
        VirtualTable virtualTable = virtualTableDao.selectVirtualTableByTableName(tableName);
        if (virtualTable == null) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_NOT_FOUND, tableName);
        }

        // convert virtual table to table field
        // virtualTable.getTableFields()
        List<TableField> tableFields = convertTableSchema(virtualTable.getTableFields());
        tableSchemaService.getAddSeaTunnelSchema(tableFields, pluginName);
        return tableFields;
    }

    private List<TableField> convertTableSchema(String virtualTableFieldJson) {
        List<TableField> fields = new ArrayList<>();
        List<VirtualTableFieldRes> virtualTableFields =
                JsonUtils.toList(virtualTableFieldJson, VirtualTableFieldRes.class);
        if (CollectionUtils.isEmpty(virtualTableFields)) {
            return fields;
        }
        virtualTableFields.forEach(
                virtualTableField -> {
                    TableField tableField = new TableField();
                    tableField.setPrimaryKey(virtualTableField.getPrimaryKey());
                    tableField.setName(virtualTableField.getFieldName());
                    tableField.setType(virtualTableField.getFieldType());
                    tableField.setComment(virtualTableField.getFieldComment());
                    tableField.setNullable(virtualTableField.getNullable());
                    tableField.setDefaultValue(virtualTableField.getDefaultValue());
                    fields.add(tableField);
                });
        return fields;
    }

    @Override
    public PageInfo<DatasourceRes> queryDatasourceList(
            Integer userId, String searchVal, String pluginName, Integer pageNo, Integer pageSize) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_LIST, userId);
        Page<Datasource> page = new Page<>(pageNo, pageSize);
        PageInfo<DatasourceRes> pageInfo = new PageInfo<>();
        List<Long> ids =
                availableResourceRange(
                        SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(), userId);
        if (org.springframework.util.CollectionUtils.isEmpty(ids)) {
            return pageInfo;
        }
        IPage<Datasource> datasourcePage =
                datasourceDao.selectDatasourceByParam(page, ids, searchVal, pluginName);
        pageInfo = new PageInfo<>();
        pageInfo.setPageNo((int) datasourcePage.getPages());
        pageInfo.setPageSize((int) datasourcePage.getSize());
        pageInfo.setTotalCount((int) datasourcePage.getTotal());
        if (CollectionUtils.isEmpty(datasourcePage.getRecords())) {
            pageInfo.setData(new ArrayList<>());
            return pageInfo;
        }
        List<Integer> userIds = new ArrayList<>();
        datasourcePage
                .getRecords()
                .forEach(
                        datasource -> {
                            userIds.add(datasource.getCreateUserId());
                            userIds.add(datasource.getUpdateUserId());
                        });

        List<DatasourceRes> datasourceResList =
                datasourcePage.getRecords().stream()
                        .map(
                                datasource -> {
                                    DatasourceRes datasourceRes = new DatasourceRes();
                                    datasourceRes.setId(datasource.getId().toString());
                                    datasourceRes.setDatasourceName(datasource.getDatasourceName());
                                    datasourceRes.setPluginName(datasource.getPluginName());
                                    datasourceRes.setPluginVersion(datasource.getPluginVersion());
                                    datasourceRes.setDescription(datasource.getDescription());
                                    datasourceRes.setCreateTime(datasource.getCreateTime());
                                    datasourceRes.setUpdateTime(datasource.getUpdateTime());
                                    Map<String, String> datasourceConfig =
                                            JsonUtils.toMap(
                                                    datasource.getDatasourceConfig(),
                                                    String.class,
                                                    String.class);
                                    datasourceRes.setDatasourceConfig(datasourceConfig);
                                    datasourceRes.setCreateUserId(datasource.getCreateUserId());
                                    datasourceRes.setUpdateUserId(datasource.getUpdateUserId());
                                    datasourceRes.setUpdateTime(datasource.getUpdateTime());
                                    return datasourceRes;
                                })
                        .collect(Collectors.toList());

        pageInfo.setData(datasourceResList);
        return pageInfo;
    }

    @Override
    public List<DataSourcePluginInfo> queryAllDatasources() {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_QUERY_ALL, 0);
        return DataSourceClientFactory.getDataSourceClient().listAllDataSources();
    }

    @Override
    public List<DataSourcePluginInfo> queryAllDatasourcesByType(Integer type) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_QUERY_ALL, 0);
        return DataSourceClientFactory.getDataSourceClient().listAllDataSources().stream()
                .map(
                        dataSourcePluginInfo ->
                                type.equals(dataSourcePluginInfo.getType())
                                        ? dataSourcePluginInfo
                                        : null)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Integer, List<DataSourcePluginInfo>> queryAllDatasourcesGroupByType(
            Boolean onlyShowVirtualDatasource) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_QUERY_ALL, 0);
        Map<Integer, List<DataSourcePluginInfo>> dataSourcePluginInfoMap = new HashMap<>();
        for (DatasourcePluginTypeEnum value : DatasourcePluginTypeEnum.values()) {
            dataSourcePluginInfoMap.put(value.getCode(), new ArrayList<>());
        }

        List<DataSourcePluginInfo> dataSourcePluginInfos =
                DataSourceClientFactory.getDataSourceClient().listAllDataSources();
        for (DataSourcePluginInfo dataSourcePluginInfo : dataSourcePluginInfos) {
            // query datasource types
            if (BooleanUtils.isNotTrue(onlyShowVirtualDatasource)) {
                List<DataSourcePluginInfo> dataSourcePluginInfoList =
                        dataSourcePluginInfoMap.computeIfAbsent(
                                dataSourcePluginInfo.getType(), k -> new ArrayList<>());
                dataSourcePluginInfoList.add(dataSourcePluginInfo);
                continue;
            }

            if (Boolean.TRUE.equals(dataSourcePluginInfo.getSupportVirtualTables())) {
                List<DataSourcePluginInfo> dataSourcePluginInfoList =
                        dataSourcePluginInfoMap.computeIfAbsent(
                                dataSourcePluginInfo.getType(), k -> new ArrayList<>());
                dataSourcePluginInfoList.add(dataSourcePluginInfo);
            }
        }
        return dataSourcePluginInfoMap;
    }

    @Override
    public Map<String, String> queryDatasourceConfigById(String datasourceId) {
        long datasourceIdLong = Long.parseLong(datasourceId);
        Datasource datasource = datasourceDao.selectDatasourceById(datasourceIdLong);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceId);
        }
        String configJson = datasource.getDatasourceConfig();
        return JsonUtils.toMap(configJson, String.class, String.class);
    }

    @Override
    public Map<String, String> queryDatasourceNameByPluginName(String pluginName) {
        Map<String, String> datasourceNameMap = new HashMap<>();
        List<Datasource> datasourceList =
                datasourceDao.selectDatasourceByPluginName(
                        pluginName, DEFAULT_DATASOURCE_PLUGIN_VERSION);
        datasourceList.forEach(
                datasource ->
                        datasourceNameMap.put(
                                datasource.getId().toString(), datasource.getDatasourceName()));
        return datasourceNameMap;
    }

    @Override
    public OptionRule queryOptionRuleByPluginName(String pluginName) {
        return DataSourceClientFactory.getDataSourceClient().queryDataSourceFieldByName(pluginName);
    }

    @Override
    public OptionRule queryVirtualTableOptionRuleByPluginName(String pluginName) {
        if (checkIsSupportVirtualTable(pluginName)) {
            return DataSourceClientFactory.getDataSourceClient()
                    .queryMetadataFieldByName(pluginName);
        }
        return OptionRule.builder().build();
    }

    @Override
    public List<DatasourceDetailRes> queryDatasourceDetailListByDatasourceIds(
            List<String> datasourceIds) {
        if (CollectionUtils.isEmpty(datasourceIds)) {
            return new ArrayList<>();
        }
        funcAndResourcePermissionCheck(
                SeatunnelFuncPermissionKeyConstant.DATASOURCE_DETAIL_LIST,
                SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                datasourceIds,
                0);
        List<Long> datasourceIdsLong =
                datasourceIds.stream().map(Long::parseLong).collect(Collectors.toList());
        List<Datasource> datasourceList = datasourceDao.selectDatasourceByIds(datasourceIdsLong);

        return convertDatasourceDetailRes(datasourceList);
    }

    private List<DatasourceDetailRes> convertDatasourceDetailRes(List<Datasource> datasourceList) {
        if (CollectionUtils.isEmpty(datasourceList)) {
            return new ArrayList<>();
        }
        List<Integer> userIds = new ArrayList<>();
        datasourceList.forEach(
                datasource -> {
                    if (null != datasource.getCreateUserId()) {
                        userIds.add(datasource.getCreateUserId());
                    }
                    if (null != datasource.getUpdateUserId()) {
                        userIds.add(datasource.getUpdateUserId());
                    }
                });
        List<DatasourceDetailRes> datasourceDetailResList = new ArrayList<>();

        datasourceList.forEach(
                datasource -> {
                    DatasourceDetailRes datasourceDetailRes = new DatasourceDetailRes();
                    datasourceDetailRes.setId(datasource.getId().toString());
                    datasourceDetailRes.setDatasourceName(datasource.getDatasourceName());
                    datasourceDetailRes.setPluginName(datasource.getPluginName());
                    datasourceDetailRes.setPluginVersion(datasource.getPluginVersion());
                    datasourceDetailRes.setDescription(datasource.getDescription());
                    datasourceDetailRes.setCreateTime(datasource.getCreateTime());
                    datasourceDetailRes.setUpdateTime(datasource.getUpdateTime());
                    Map<String, String> config = JsonUtils.toMap(datasource.getDatasourceConfig());
                    datasourceDetailRes.setDatasourceConfig(config);
                    datasourceDetailResList.add(datasourceDetailRes);
                });
        return datasourceDetailResList;
    }

    @Override
    public List<DatasourceDetailRes> queryAllDatasourcesInstance() {
        List<Datasource> datasourceList = datasourceDao.queryAll();
        return convertDatasourceDetailRes(datasourceList);
    }

    @Override
    public DatasourceDetailRes queryDatasourceDetailByDatasourceName(String datasourceName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_DETAIL, 0);
        Datasource datasource = datasourceDao.queryDatasourceByName(datasourceName);
        // @cc liuli
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceName);
        }
        DatasourceDetailRes datasourceDetailRes = new DatasourceDetailRes();
        datasourceDetailRes.setId(datasource.getId().toString());
        datasourceDetailRes.setDatasourceName(datasource.getDatasourceName());
        datasourceDetailRes.setPluginName(datasource.getPluginName());
        datasourceDetailRes.setPluginVersion(datasource.getPluginVersion());
        datasourceDetailRes.setDescription(datasource.getDescription());
        datasourceDetailRes.setCreateTime(datasource.getCreateTime());
        datasourceDetailRes.setUpdateTime(datasource.getUpdateTime());

        Map<String, String> datasourceConfig =
                JsonUtils.toMap(datasource.getDatasourceConfig(), String.class, String.class);
        // convert option rule
        datasourceDetailRes.setDatasourceConfig(datasourceConfig);

        return datasourceDetailRes;
    }

    @Override
    public DatasourceDetailRes queryDatasourceDetailById(String datasourceId) {
        long datasourceIdLong = Long.parseLong(datasourceId);
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.DATASOURCE_DETAIL, 0);
        Datasource datasource = datasourceDao.selectDatasourceById(datasourceIdLong);
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_FOUND, datasourceId);
        }
        DatasourceDetailRes datasourceDetailRes = new DatasourceDetailRes();
        datasourceDetailRes.setId(datasource.getId().toString());
        datasourceDetailRes.setDatasourceName(datasource.getDatasourceName());
        datasourceDetailRes.setPluginName(datasource.getPluginName());
        datasourceDetailRes.setPluginVersion(datasource.getPluginVersion());
        datasourceDetailRes.setDescription(datasource.getDescription());
        datasourceDetailRes.setCreateTime(datasource.getCreateTime());
        datasourceDetailRes.setUpdateTime(datasource.getUpdateTime());

        Map<String, String> datasourceConfig =
                JsonUtils.toMap(datasource.getDatasourceConfig(), String.class, String.class);
        // convert option rule
        datasourceDetailRes.setDatasourceConfig(datasourceConfig);

        return datasourceDetailRes;
    }

    @Override
    public DatasourceDetailRes queryDatasourceDetailById(Integer userId, String datasourceId) {
        // check user
        funcAndResourcePermissionCheck(
                SeatunnelFuncPermissionKeyConstant.DATASOURCE_DETAIL,
                SeatunnelResourcePermissionModuleEnum.DATASOURCE.name(),
                Collections.singletonList(Long.parseLong(datasourceId)),
                userId);
        return this.queryDatasourceDetailById(datasourceId);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
