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
import org.apache.seatunnel.app.bean.connector.ConnectorCache;
import org.apache.seatunnel.app.config.ConnectorDataSourceMapperConfig;
import org.apache.seatunnel.app.domain.request.connector.BusinessMode;
import org.apache.seatunnel.app.domain.request.connector.ConnectorStatus;
import org.apache.seatunnel.app.domain.request.connector.SceneMode;
import org.apache.seatunnel.app.domain.request.job.transform.Transform;
import org.apache.seatunnel.app.domain.response.connector.ConnectorInfo;
import org.apache.seatunnel.app.domain.response.connector.DataSourceInfo;
import org.apache.seatunnel.app.domain.response.connector.DataSourceInstance;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.permission.constants.SeatunnelFuncPermissionKeyConstant;
import org.apache.seatunnel.app.service.IConnectorService;
import org.apache.seatunnel.app.service.IDatasourceService;
import org.apache.seatunnel.app.service.IJobDefinitionService;
import org.apache.seatunnel.app.thirdparty.datasource.DataSourceConfigSwitcherUtils;
import org.apache.seatunnel.app.thirdparty.transfrom.TransformConfigSwitcherUtils;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;

import javax.annotation.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConnectorServiceImpl extends SeatunnelBaseServiceImpl implements IConnectorService {

    private final ConnectorCache connectorCache;

    @Autowired private ConnectorDataSourceMapperConfig dataSourceMapperConfig;

    @Resource private IDatasourceService datasourceService;

    @Resource private IJobDefinitionService jobDefinitionService;

    private static final List<String> SKIP_SOURCE = Collections.emptyList();

    private static final List<String> SKIP_SINK = Collections.singletonList("Console");

    @Autowired
    public ConnectorServiceImpl(ConnectorCache connectorCache) {
        this.connectorCache = connectorCache;
    }

    @Override
    public List<ConnectorInfo> listSources(ConnectorStatus status) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_SOURCES, 0);
        List<ConnectorInfo> connectorInfos;
        if (status == ConnectorStatus.ALL) {
            connectorInfos = connectorCache.getAllConnectors(PluginType.SOURCE);
        } else if (status == ConnectorStatus.DOWNLOADED) {
            connectorInfos = connectorCache.getDownLoadConnector(PluginType.SOURCE);
        } else {
            connectorInfos = connectorCache.getNotDownLoadConnector(PluginType.SOURCE);
        }
        return connectorInfos.stream()
                .filter(c -> !SKIP_SOURCE.contains(c.getPluginIdentifier().getPluginName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSourceInstance> listSourceDataSourceInstances(
            Long jobId, SceneMode sceneMode, ConnectorStatus status) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_DATASOURCE_SOURCES, 0);
        BusinessMode businessMode =
                BusinessMode.valueOf(
                        jobDefinitionService
                                .getJobDefinitionByJobId(jobId)
                                .getJobType()
                                .toUpperCase());

        return filterImplementDataSource(
                        listSources(status), businessMode, sceneMode, PluginType.SOURCE)
                .stream()
                .flatMap(dataSourceInfo -> getDataSourcesInstance(dataSourceInfo).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<ConnectorInfo> listTransforms() {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_TRANSFORMS, 0);
        return connectorCache.getTransform();
    }

    @Override
    public List<ConnectorInfo> listTransformsForJob(Long jobId) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_DATASOURCE_TRANSFORMS, 0);
        BusinessMode businessMode =
                BusinessMode.valueOf(
                        jobDefinitionService
                                .getJobDefinitionByJobId(jobId)
                                .getJobType()
                                .toUpperCase());

        if (businessMode.equals(BusinessMode.DATA_INTEGRATION)) {
            return connectorCache.getTransform().stream()
                    .filter(
                            connectorInfo -> {
                                String pluginName =
                                        connectorInfo.getPluginIdentifier().getPluginName();
                                return pluginName.equals("FieldMapper")
                                        || pluginName.equals("FilterRowKind")
                                        || pluginName.equals("Replace")
                                        || pluginName.equals("Copy")
                                        || pluginName.equals("MultiFieldSplit")
                                        || pluginName.equals("Sql");
                            })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public List<ConnectorInfo> listSinks(ConnectorStatus status) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_SINKS, 0);
        List<ConnectorInfo> connectorInfos;
        if (status == ConnectorStatus.ALL) {
            connectorInfos = connectorCache.getAllConnectors(PluginType.SINK);
        } else if (status == ConnectorStatus.DOWNLOADED) {
            connectorInfos = connectorCache.getDownLoadConnector(PluginType.SINK);
        } else {
            connectorInfos = connectorCache.getNotDownLoadConnector(PluginType.SINK);
        }
        return connectorInfos.stream()
                .filter(c -> !SKIP_SINK.contains(c.getPluginIdentifier().getPluginName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DataSourceInstance> listSinkDataSourcesInstances(
            Long jobId, ConnectorStatus status) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_DATASOURCE_SINKS, 0);

        BusinessMode businessMode =
                BusinessMode.valueOf(
                        jobDefinitionService
                                .getJobDefinitionByJobId(jobId)
                                .getJobType()
                                .toUpperCase());

        return filterImplementDataSource(listSinks(status), businessMode, null, PluginType.SINK)
                .stream()
                .flatMap(dataSourceInfo -> getDataSourcesInstance(dataSourceInfo).stream())
                .collect(Collectors.toList());
    }

    @Override
    public void sync() throws IOException {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_SYNC, 0);
        connectorCache.refresh();
    }

    @Override
    public FormStructure getConnectorFormStructure(
            @NonNull String pluginType, @NonNull String connectorName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_FORM, 0);
        FormStructure formStructure = connectorCache.getFormStructure(pluginType, connectorName);
        if (formStructure == null) {
            throw new SeatunnelException(
                    SeatunnelErrorEnum.CONNECTOR_NOT_FOUND, pluginType, connectorName);
        }

        return formStructure;
    }

    @Override
    public FormStructure getTransformFormStructure(
            @NonNull String pluginType, @NonNull String connectorName) {
        OptionRule optionRule = connectorCache.getOptionRule(pluginType, connectorName);
        return TransformConfigSwitcherUtils.getFormStructure(
                Transform.valueOf(connectorName.toUpperCase()), optionRule);
    }

    @Override
    public FormStructure getDatasourceFormStructure(
            @NonNull Long jobId, @NonNull Long dataSourceInstanceId, @NonNull String pluginType) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.CONNECTOR_DATASOURCE_FORM, 0);
        BusinessMode businessMode =
                BusinessMode.valueOf(
                        jobDefinitionService
                                .getJobDefinitionByJobId(jobId)
                                .getJobType()
                                .toUpperCase());

        // 1.dataSourceInstanceId query dataSourceName,
        String dataSourceName =
                datasourceService
                        .queryDatasourceDetailById(dataSourceInstanceId.toString())
                        .getPluginName();

        // 2.DataSourceName DataSourceName OptionRole
        OptionRule dataSourceNameOptionRole =
                datasourceService.queryOptionRuleByPluginName(dataSourceName);

        // 3.OptionRole
        OptionRule virtualTableOptionRule =
                datasourceService.queryVirtualTableOptionRuleByPluginName(dataSourceName);

        // 4.dataSourceName query connector
        String connectorName =
                dataSourceMapperConfig.findConnectorForDatasourceName(dataSourceName).get();

        // 5.call utils
        PluginType connectorPluginType = PluginType.valueOf(pluginType.toUpperCase(Locale.ROOT));

        return DataSourceConfigSwitcherUtils.filterOptionRule(
                dataSourceName,
                connectorName,
                dataSourceNameOptionRole,
                virtualTableOptionRule,
                connectorPluginType,
                businessMode,
                connectorCache.getOptionRule(pluginType, connectorName));
    }

    /**
     * Filter connectors that have a datasource implementation and Supported BusinessMode and
     * sceneModes
     *
     * @return Connectors with data source implementations
     */
    private List<DataSourceInfo> filterImplementDataSource(
            List<ConnectorInfo> connectorInfoS,
            BusinessMode businessMode,
            SceneMode sceneMode,
            PluginType pluginType) {
        List<DataSourceInfo> dataSourceList = new ArrayList<>();
        connectorInfoS.forEach(
                connectorInfo -> {
                    String connectorName = connectorInfo.getPluginIdentifier().getPluginName();
                    ConnectorDataSourceMapperConfig.ConnectorMapper connectorMapper =
                            dataSourceMapperConfig
                                    .getConnectorDatasourceMappers()
                                    .get(connectorName);

                    if (null != connectorMapper) {
                        connectorMapper
                                .getDataSources()
                                .forEach(
                                        datasourceName -> {
                                            Optional<List<SceneMode>> sceneModes =
                                                    dataSourceMapperConfig.supportedSceneMode(
                                                            datasourceName, pluginType);
                                            Optional<List<BusinessMode>> businessModes =
                                                    dataSourceMapperConfig.supportedBusinessMode(
                                                            datasourceName, pluginType);
                                            if ((businessMode == null
                                                            || businessModes.isPresent()
                                                                    && businessModes
                                                                            .get()
                                                                            .contains(businessMode))
                                                    && (sceneMode == null
                                                            || (sceneModes.isPresent()
                                                                    && sceneModes
                                                                            .get()
                                                                            .contains(
                                                                                    sceneMode)))) {
                                                dataSourceList.add(
                                                        new DataSourceInfo(
                                                                connectorInfo, datasourceName));
                                            }
                                        });
                    }
                });

        return dataSourceList;
    }

    /** Get an instance from a data source */
    private List<DataSourceInstance> getDataSourcesInstance(DataSourceInfo dataSourceInfo) {

        // dataSourceName DataSourceInstance
        return datasourceService.queryDatasourceNameByPluginName(dataSourceInfo.getDatasourceName())
                .entrySet().stream()
                .map(
                        en -> {
                            return new DataSourceInstance(
                                    dataSourceInfo, en.getValue(), Long.parseLong(en.getKey()));
                        })
                .collect(Collectors.toList());
    }
}
