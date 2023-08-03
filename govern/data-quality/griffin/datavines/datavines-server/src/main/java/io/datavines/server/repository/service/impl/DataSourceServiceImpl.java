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
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.datavines.common.utils.*;
import io.datavines.core.utils.LanguageUtils;
import io.datavines.server.api.dto.bo.catalog.CatalogRefresh;
import io.datavines.server.api.dto.bo.datasource.ExecuteRequest;
import io.datavines.common.exception.DataVinesException;
import io.datavines.common.param.*;
import io.datavines.connector.api.ConnectorFactory;
import io.datavines.core.enums.Status;
import io.datavines.server.api.dto.bo.datasource.DataSourceCreate;
import io.datavines.server.api.dto.bo.datasource.DataSourceUpdate;
import io.datavines.server.api.dto.vo.DataSourceVO;
import io.datavines.server.repository.entity.DataSource;
import io.datavines.server.repository.mapper.DataSourceMapper;
import io.datavines.server.repository.service.*;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.utils.ContextHolder;
import io.datavines.spi.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.datavines.common.log.SensitiveDataConverter.PWD_PATTERN_1;

@Slf4j
@Service("dataSourceService")
public class DataSourceServiceImpl extends ServiceImpl<DataSourceMapper, DataSource>  implements DataSourceService {

    @Autowired
    private JobService jobService;

    @Autowired
    private CatalogMetaDataFetchTaskService catalogMetaDataFetchTaskService;

    @Autowired
    private CatalogEntityInstanceService catalogEntityInstanceService;

    @Override
    public boolean testConnect(TestConnectionRequestParam param) {
        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(param.getType());
        ConnectorResponse response = connectorFactory.getConnector().testConnect(param);
        return (boolean)response.getResult();
    }

    @Override
    public long insert(DataSourceCreate dataSourceCreate) {
        DataSource dataSource = new DataSource();
        BeanUtils.copyProperties(dataSourceCreate, dataSource);
        String param = dataSourceCreate.getParam();
        String paramCode = "";

        Map<String,String> paramMap = JSONUtils.toMap(param);

        if (MapUtils.isEmpty(paramMap)) {
            return -1L;
        }

        String type = dataSourceCreate.getType();

        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(type);
        List<String> keyProperties = connectorFactory.getConnector().keyProperties();
        List<String> keyPropertyValueList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(keyProperties)) {
            keyProperties.forEach(property -> {
                if (StringUtils.isNotEmpty(paramMap.get(property))) {
                    keyPropertyValueList.add(paramMap.get(property).toLowerCase());
                }
            });
        }

        if (CollectionUtils.isNotEmpty(keyPropertyValueList)) {
            paramCode = Md5Utils.getMd5(String.join("@#@", keyPropertyValueList),true);
        }

        try {
            param = CryptionUtils.encryptByAES(param, CommonPropertyUtils.getString(CommonPropertyUtils.AES_KEY, CommonPropertyUtils.AES_KEY_DEFAULT));
        } catch (Exception e) {
            throw new DataVinesException("encrypt datasource param error : {}", e);
        }

        dataSource.setUuid(UUID.randomUUID().toString());
        dataSource.setParam(param);
        dataSource.setParamCode(paramCode);
        dataSource.setCreateTime(LocalDateTime.now());
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSource.setCreateBy(ContextHolder.getUserId());
        dataSource.setUpdateBy(ContextHolder.getUserId());
        baseMapper.insert(dataSource);

        CatalogRefresh catalogRefresh = new CatalogRefresh();
        catalogRefresh.setDatasourceId(dataSource.getId());
        catalogMetaDataFetchTaskService.refreshCatalog(catalogRefresh);
        return dataSource.getId();
    }

    @Override
    public int update(DataSourceUpdate dataSourceUpdate) throws DataVinesException {
        DataSource dataSource = baseMapper.selectById(dataSourceUpdate.getId());
        if (dataSource == null){
            throw new DataVinesException("can not find the datasource");
        }

        BeanUtils.copyProperties(dataSourceUpdate, dataSource);
        String param = dataSourceUpdate.getParam();

        String paramCode = "";

        Map<String,String> paramMap = JSONUtils.toMap(param);

        if (MapUtils.isEmpty(paramMap)) {
            return -1;
        }

        String type = dataSourceUpdate.getType();

        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(type);
        List<String> keyProperties = connectorFactory.getConnector().keyProperties();
        List<String> keyPropertyValueList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(keyProperties)) {
            keyProperties.forEach(property -> {
                if (StringUtils.isNotEmpty(paramMap.get(property))) {
                    keyPropertyValueList.add(paramMap.get(property).toLowerCase());
                }
            });
        }

        if (CollectionUtils.isNotEmpty(keyPropertyValueList)) {
            paramCode = Md5Utils.getMd5(String.join("@#@", keyPropertyValueList),true);
        }

        try {
            param = CryptionUtils.encryptByAES(param
                    ,CommonPropertyUtils.getString(CommonPropertyUtils.AES_KEY, CommonPropertyUtils.AES_KEY_DEFAULT));
        } catch (Exception e) {
            throw new DataVinesException("encrypt datasource param error : {}", e);
        }

        dataSource.setParam(param);
        dataSource.setParamCode(paramCode);
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSource.setUpdateBy(ContextHolder.getUserId());

        return baseMapper.updateById(dataSource);
    }

    @Override
    public DataSource getDataSourceById(long id) {
        DataSource dataSourceVO = new DataSource();

        DataSource dataSource = baseMapper.selectById(id);
        if (dataSource == null) {
            return null;
        }

        BeanUtils.copyProperties(dataSource, dataSourceVO);

        String param = dataSource.getParam();

        try {
            param = CryptionUtils.decryptByAES(param
                    ,CommonPropertyUtils.getString(CommonPropertyUtils.AES_KEY, CommonPropertyUtils.AES_KEY_DEFAULT));
        } catch (Exception e) {
            throw new DataVinesException("encrypt datasource param error : ", e);
        }

        dataSourceVO.setParam(param);

        return dataSourceVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(long id) {
        DataSource dataSource = getById(id);
        if (dataSource != null) {
            catalogEntityInstanceService.deleteEntityByUUID(dataSource.getUuid());
            jobService.deleteByDataSourceId(id);
            catalogMetaDataFetchTaskService.deleteByDataSourceId(id);
            removeById(id);
            return 1;
        }

        return 0;
    }

    @Override
    public IPage<DataSourceVO> getDataSourcePage(String searchVal, Long workspaceId, Integer pageNumber, Integer pageSize) {
        Page<DataSourceVO> page = new Page<>(pageNumber, pageSize);
        IPage<DataSourceVO> dataSources = baseMapper.getDataSourcePage(page, searchVal, workspaceId);
        dataSources.getRecords().forEach(dataSourceVO -> {
            String param = dataSourceVO.getParam();

            try {
                param = CryptionUtils.decryptByAES(param
                        ,CommonPropertyUtils.getString(CommonPropertyUtils.AES_KEY, CommonPropertyUtils.AES_KEY_DEFAULT));
            } catch (Exception e) {
                throw new DataVinesException("encrypt datasource param error : {}", e);
            }

            dataSourceVO.setParam(PasswordFilterUtils.convertPasswordToNULL(PWD_PATTERN_1, param));
        });
        return dataSources;
    }

    @Override
    public List<DataSource> listByWorkSpaceId(long workspaceId) {
        return baseMapper.selectList(new QueryWrapper<DataSource>().eq("workspace_id", workspaceId));
    }

    @Override
    public List<DataSource> listByWorkSpaceIdAndType(long workspaceId, String type) {
        return baseMapper.selectList(new QueryWrapper<DataSource>().eq("workspace_id", workspaceId).eq("type", type));
    }

    @Override
    public Object getDatabaseList(Long id) throws DataVinesServerException {

        DataSource dataSource = getDataSourceById(id);
        GetDatabasesRequestParam param = new GetDatabasesRequestParam();
        param.setType(dataSource.getType());
        param.setDataSourceParam(dataSource.getParam());

        Object result = null;
        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(param.getType());
        try {
            ConnectorResponse response = connectorFactory.getConnector().getDatabases(param);
            result = response.getResult();
        } catch (SQLException e) {
            log.error(MessageFormat.format(Status.GET_DATABASE_LIST_ERROR.getMsg(), dataSource.getName()), e);
            throw new DataVinesServerException(Status.GET_DATABASE_LIST_ERROR, dataSource.getName());
        }

        return result;
    }

    @Override
    public Object getTableList(Long id, String database) throws DataVinesServerException {
        DataSource dataSource = getDataSourceById(id);
        GetTablesRequestParam param = new GetTablesRequestParam();
        param.setType(dataSource.getType());
        param.setDataSourceParam(dataSource.getParam());
        param.setDataBase(database);

        Object result = null;
        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(param.getType());
        try {
            ConnectorResponse response = connectorFactory.getConnector().getTables(param);
            result = response.getResult();
        } catch (SQLException e) {
            log.error(MessageFormat.format(Status.GET_TABLE_LIST_ERROR.getMsg(), dataSource.getName(), database), e);
            throw new DataVinesServerException(Status.GET_TABLE_LIST_ERROR, dataSource.getName(), database);
        }

        return result;
    }

    @Override
    public Object getColumnList(Long id, String database, String table) throws DataVinesServerException {
        DataSource dataSource = getDataSourceById(id);
        GetColumnsRequestParam param = new GetColumnsRequestParam();
        param.setType(dataSource.getType());
        param.setDataSourceParam(dataSource.getParam());
        param.setDataBase(database);
        param.setTable(table);

        Object result = null;
        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(param.getType());
        try {
            ConnectorResponse response = connectorFactory.getConnector().getColumns(param);
            result = response.getResult();
        } catch (SQLException e) {
            log.error(MessageFormat.format(Status.GET_COLUMN_LIST_ERROR.getMsg(), dataSource.getName(), database, table), e);
            throw new DataVinesServerException(Status.GET_COLUMN_LIST_ERROR, dataSource.getName(), database, table);
        }

        return result;
    }

    @Override
    public Object executeScript(ExecuteRequest request) throws DataVinesServerException {
        DataSource dataSource = getDataSourceById(request.getDatasourceId());
        ExecuteRequestParam param = new ExecuteRequestParam();
        param.setType(dataSource.getType());
        param.setDataSourceParam(dataSource.getParam());
        param.setScript(request.getScript());
        Object result = null;
        ConnectorFactory connectorFactory = PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(param.getType());
        try {
            ConnectorResponse response = connectorFactory.getExecutor().executeSyncQuery(param);
            result = response.getResult();
        } catch (SQLException e) {
            log.error(MessageFormat.format(Status.EXECUTE_SCRIPT_ERROR.getMsg(), request.getScript()), e);
            throw new DataVinesServerException(Status.GET_TABLE_LIST_ERROR, request.getScript());
        }

        return result;
    }

    @Override
    public String getConfigJson(String type) {
        return PluginLoader.getPluginLoader(ConnectorFactory.class).getOrCreatePlugin(type).getConnector().getConfigJson(!LanguageUtils.isZhContext());
    }
}
