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
import org.apache.seatunnel.app.domain.request.datasource.VirtualTableFieldReq;
import org.apache.seatunnel.app.domain.request.datasource.VirtualTableReq;
import org.apache.seatunnel.app.domain.response.PageInfo;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableDetailRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableFieldRes;
import org.apache.seatunnel.app.domain.response.datasource.VirtualTableRes;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.permission.constants.SeatunnelFuncPermissionKeyConstant;
import org.apache.seatunnel.app.service.IJobDefinitionService;
import org.apache.seatunnel.app.service.IVirtualTableService;
import org.apache.seatunnel.app.thirdparty.datasource.DataSourceClientFactory;
import org.apache.seatunnel.app.thirdparty.framework.SeaTunnelOptionRuleWrapper;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.server.common.CodeGenerateUtils;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VirtualTableServiceImpl extends SeatunnelBaseServiceImpl
        implements IVirtualTableService {

    @Resource(name = "virtualTableDaoImpl")
    IVirtualTableDao virtualTableDao;

    @Autowired private IJobDefinitionService jobDefinitionService;

    @Resource(name = "datasourceDaoImpl")
    IDatasourceDao datasourceDao;

    @Autowired private ConnectorDataSourceMapperConfig dataSourceMapperConfig;

    @Override
    public String createVirtualTable(Integer userId, VirtualTableReq req)
            throws CodeGenerateUtils.CodeGenerateException {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.VIRTUAL_TABLE_CREATE, userId);
        // check user has permission to create virtual table
        long uuid = CodeGenerateUtils.getInstance().genCode();
        Long datasourceId = Long.valueOf(req.getDatasourceId());
        boolean isUnique =
                virtualTableDao.checkVirtualTableNameUnique(
                        req.getTableName(), req.getDatabaseName(), 0L);
        if (!isUnique) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.VIRTUAL_TABLE_ALREADY_EXISTS, req.getTableName());
        }

        VirtualTable virtualTable =
                VirtualTable.builder()
                        .id(uuid)
                        .datasourceId(datasourceId)
                        .virtualDatabaseName(req.getDatabaseName())
                        .virtualTableName(req.getTableName())
                        .description(req.getDescription())
                        .createTime(new Date())
                        .updateTime(new Date())
                        .createUserId(userId)
                        .updateUserId(userId)
                        .build();
        if (CollectionUtils.isEmpty(req.getTableFields())) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_FIELD_EMPTY);
        }
        String fieldJson = convertTableFields(req.getTableFields());
        virtualTable.setTableFields(fieldJson);
        virtualTable.setVirtualTableConfig(JsonUtils.toJsonString(req.getDatabaseProperties()));

        boolean success = virtualTableDao.insertVirtualTable(virtualTable);
        if (!success) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_CREATE_FAILED);
        }
        return String.valueOf(uuid);
    }

    private String convertTableFields(List<VirtualTableFieldReq> tableFields) {
        List<VirtualTableFieldRes> fieldList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(tableFields)) {
            for (VirtualTableFieldReq field : tableFields) {
                VirtualTableFieldRes fieldRes =
                        VirtualTableFieldRes.builder()
                                .fieldName(field.getFieldName())
                                .fieldType(field.getFieldType())
                                .fieldExtra(field.getFieldExtra())
                                .defaultValue(field.getDefaultValue())
                                .primaryKey(field.getPrimaryKey())
                                .nullable(field.getNullable())
                                .fieldComment(field.getFieldComment())
                                .build();
                fieldList.add(fieldRes);
            }
        }
        return JsonUtils.toJsonString(fieldList);
    }

    @Override
    public Boolean updateVirtualTable(
            @NotNull Integer userId, @NotNull String tableId, VirtualTableReq req) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.VIRTUAL_TABLE_UPDATE, userId);
        VirtualTable originalTable = virtualTableDao.selectVirtualTableById(Long.valueOf(tableId));
        if (null == originalTable) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_NOT_EXISTS);
        }
        if (StringUtils.isNotBlank(req.getTableName())) {
            boolean isUnique =
                    virtualTableDao.checkVirtualTableNameUnique(
                            req.getTableName(), req.getDatabaseName(), Long.valueOf(tableId));
            if (!isUnique) {
                throw new SeatunnelException(
                        SeatunnelErrorEnum.VIRTUAL_TABLE_ALREADY_EXISTS, req.getTableName());
            }
        }

        VirtualTable virtualTable =
                VirtualTable.builder()
                        .id(Long.valueOf(tableId))
                        .datasourceId(originalTable.getDatasourceId())
                        .virtualDatabaseName(req.getDatabaseName())
                        .virtualTableName(req.getTableName())
                        .description(req.getDescription())
                        .updateTime(new Date())
                        .updateUserId(userId)
                        .build();
        if (CollectionUtils.isNotEmpty(req.getTableFields())) {
            String fieldJson = convertTableFields(req.getTableFields());
            virtualTable.setTableFields(fieldJson);
        }

        virtualTable.setVirtualTableConfig(JsonUtils.toJsonString(req.getDatabaseProperties()));

        return virtualTableDao.updateVirtualTable(virtualTable);
    }

    @Override
    public Boolean deleteVirtualTable(@NotNull Integer userId, @NotNull String tableId) {
        // todo  check has permission and has job using this table
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.VIRTUAL_TABLE_DELETE, userId);
        VirtualTable virtualTable = virtualTableDao.selectVirtualTableById(Long.valueOf(tableId));
        if (virtualTable == null) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_NOT_EXISTS);
        }
        if (jobDefinitionService.getUsedByDataSourceIdAndVirtualTable(
                virtualTable.getDatasourceId(), virtualTable.getVirtualTableName())) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_CAN_NOT_DELETE);
        }
        return virtualTableDao.deleteVirtualTable(Long.valueOf(tableId));
    }

    @Override
    public Boolean checkVirtualTableValid(VirtualTableReq req) {
        // confirm need to do?
        return true;
    }

    @Override
    public String queryTableDynamicTable(String pluginName) {
        OptionRule rule =
                DataSourceClientFactory.getDataSourceClient().queryMetadataFieldByName(pluginName);
        if (null == rule) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_EXISTS);
        }
        String connectorForDatasourceName =
                dataSourceMapperConfig
                        .findConnectorForDatasourceName(pluginName)
                        .orElseThrow(
                                () ->
                                        new SeatunnelException(
                                                SeatunnelErrorEnum
                                                        .CAN_NOT_FOUND_CONNECTOR_FOR_DATASOURCE,
                                                pluginName));
        FormStructure form = SeaTunnelOptionRuleWrapper.wrapper(rule, connectorForDatasourceName);
        return JsonUtils.toJsonString(form);
    }

    @Override
    public VirtualTableDetailRes queryVirtualTableByTableName(String tableName) {
        VirtualTable virtualTable = virtualTableDao.selectVirtualTableByTableName(tableName);
        return buildVirtualTableDetailRes(virtualTable);
    }

    @Override
    public boolean containsVirtualTableByTableName(String tableName) {
        return null != virtualTableDao.selectVirtualTableByTableName(tableName);
    }

    private VirtualTableDetailRes buildVirtualTableDetailRes(VirtualTable virtualTable) {
        if (null == virtualTable) {
            throw new SeatunnelException(SeatunnelErrorEnum.VIRTUAL_TABLE_NOT_EXISTS);
        }
        Datasource datasource = datasourceDao.selectDatasourceById(virtualTable.getDatasourceId());
        if (null == datasource) {
            throw new SeatunnelException(SeatunnelErrorEnum.DATASOURCE_NOT_EXISTS);
        }

        VirtualTableDetailRes res = new VirtualTableDetailRes();
        res.setTableId(String.valueOf(virtualTable.getId()));
        res.setTableName(virtualTable.getVirtualTableName());
        res.setDatabaseName(virtualTable.getVirtualDatabaseName());
        res.setDescription(virtualTable.getDescription());
        res.setDatasourceId(String.valueOf(virtualTable.getDatasourceId()));
        res.setPluginName(datasource.getPluginName());
        res.setCreateTime(virtualTable.getCreateTime());
        res.setUpdateTime(virtualTable.getUpdateTime());
        res.setDatasourceName(datasource.getDatasourceName());
        res.setDatasourceProperties(JsonUtils.toMap(virtualTable.getVirtualTableConfig()));

        List<VirtualTableFieldRes> fields =
                JsonUtils.toList(virtualTable.getTableFields(), VirtualTableFieldRes.class);
        res.setFields(fields);
        return res;
    }

    @Override
    public VirtualTableDetailRes queryVirtualTable(@NotNull String tableId) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.VIRTUAL_TABLE_DETAIL, 0);
        VirtualTable virtualTable = virtualTableDao.selectVirtualTableById(Long.valueOf(tableId));
        return buildVirtualTableDetailRes(virtualTable);
    }

    @Override
    public PageInfo<VirtualTableRes> getVirtualTableList(
            String pluginName, String datasourceName, Integer pageNo, Integer pageSize) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.VIRTUAL_TABLE_VIEW, 0);
        Page<VirtualTable> page = new Page<>(pageNo, pageSize);
        IPage<VirtualTable> iPage =
                virtualTableDao.selectVirtualTablePage(page, pluginName, datasourceName);

        PageInfo<VirtualTableRes> pageInfo = new PageInfo<>();
        pageInfo.setPageNo((int) iPage.getPages());
        pageInfo.setPageSize((int) iPage.getSize());
        pageInfo.setTotalCount((int) iPage.getTotal());

        List<VirtualTableRes> resList = new ArrayList<>();
        List<Long> datasourceIds = new ArrayList<>();
        if (CollectionUtils.isEmpty(iPage.getRecords())) {
            pageInfo.setData(resList);
            return pageInfo;
        }
        iPage.getRecords()
                .forEach(
                        virtualTable -> {
                            datasourceIds.add(virtualTable.getDatasourceId());
                        });
        List<Datasource> datasourceList = datasourceDao.selectDatasourceByIds(datasourceIds);
        Map<Long, String> datasourceMap =
                datasourceList.stream()
                        .collect(
                                Collectors.toMap(Datasource::getId, Datasource::getDatasourceName));
        Map<Long, String> datasourcePluginNameMap =
                datasourceList.stream()
                        .collect(Collectors.toMap(Datasource::getId, Datasource::getPluginName));
        iPage.getRecords()
                .forEach(
                        virtualTable -> {
                            VirtualTableRes res = new VirtualTableRes();
                            res.setTableId(String.valueOf(virtualTable.getId()));
                            res.setTableName(virtualTable.getVirtualTableName());
                            res.setDatabaseName(virtualTable.getVirtualDatabaseName());
                            res.setDescription(virtualTable.getDescription());
                            res.setDatasourceId(String.valueOf(virtualTable.getDatasourceId()));
                            res.setCreateUserId(virtualTable.getCreateUserId());
                            res.setUpdateUserId(virtualTable.getUpdateUserId());
                            res.setCreateTime(virtualTable.getCreateTime());
                            res.setUpdateTime(virtualTable.getUpdateTime());

                            res.setDatasourceName(
                                    datasourceMap.get(virtualTable.getDatasourceId()));
                            res.setPluginName(
                                    datasourcePluginNameMap.get(virtualTable.getDatasourceId()));
                            resList.add(res);
                        });
        pageInfo.setData(resList);
        return pageInfo;
    }

    @Override
    public List<String> getVirtualTableNames(String databaseName, String datasourceId) {
        Long datasourceIdLong = Long.valueOf(datasourceId);
        return virtualTableDao.getVirtualTableNames(databaseName, datasourceIdLong);
    }

    @Override
    public List<String> getVirtualDatabaseNames(String datasourceId) {
        Long datasourceIdLong = Long.valueOf(datasourceId);
        return virtualTableDao.getVirtualDatabaseNames(datasourceIdLong);
    }

    private VirtualTableFieldRes convertVirtualTableFieldReq(VirtualTableFieldReq req) {

        return VirtualTableFieldRes.builder()
                .fieldName(req.getFieldName())
                .fieldType(req.getFieldType())
                .nullable(req.getNullable())
                .defaultValue(req.getDefaultValue())
                .fieldComment(req.getFieldComment())
                .primaryKey(req.getPrimaryKey())
                .build();
    }

    /* private VirtualTableDetailRes convertVirtualTableDetailResponse(VirtualTableDetailResponse response) {
        VirtualTableDetailRes res = new VirtualTableDetailRes();
        res.setTableId(response.getId().toString());
        res.setTableName(response.getTableName());
        res.setDatabaseName(response.getDatabaseName());
        res.setDescription(response.getDescription());
        res.setCreateTime(response.getCreateTime());
        res.setUpdateTime(response.getUpdateTime());

        res.setFields(convertVirtualTableFieldResponse(response.getFields()));

        List<Integer> userIds = new ArrayList<>();
        userIds.add(Integer.parseInt(response.getCreateUserId()));
        userIds.add(Integer.parseInt(response.getUpdateUserId()));
        Map<Integer, String> userNames = queryUserNamesByIds(userIds);
        res.setCreateUserName(userNames.get(Integer.parseInt(response.getCreateUserId())));
        res.setUpdateUserName(userNames.get(Integer.parseInt(response.getUpdateUserId())));
        return res;
    }

    private List<VirtualTableFieldRes> convertVirtualTableFieldResponse(List<VirtualTableFieldResponse> fieldResponses) {
        List<VirtualTableFieldRes> fields = new ArrayList<>();
        if (fieldResponses != null && !fieldResponses.isEmpty()) {
            for (VirtualTableFieldResponse fieldResponse : fieldResponses) {
                VirtualTableFieldRes field = new VirtualTableFieldRes();
                field.setFieldName(fieldResponse.getName());
                field.setFieldType(fieldResponse.getType());
                field.setNullable(fieldResponse.getNullable());
                field.setDefaultValue(fieldResponse.getDefaultValue());
                field.setFieldComment(fieldResponse.getComment());
                field.setPrimaryKey(fieldResponse.getPrimaryKey());
                fields.add(field);
            }
        }
        return fields;
    }*/
}
