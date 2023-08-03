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

import org.apache.seatunnel.api.table.catalog.DataTypeConvertor;
import org.apache.seatunnel.api.table.factory.DataTypeConvertorFactory;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.app.bean.connector.ConnectorCache;
import org.apache.seatunnel.app.config.ConnectorDataSourceMapperConfig;
import org.apache.seatunnel.app.domain.request.job.DataSourceOption;
import org.apache.seatunnel.app.domain.request.job.TableSchemaReq;
import org.apache.seatunnel.app.domain.response.job.TableSchemaRes;
import org.apache.seatunnel.app.permission.constants.SeatunnelFuncPermissionKeyConstant;
import org.apache.seatunnel.app.service.IDatasourceService;
import org.apache.seatunnel.app.service.ITableSchemaService;
import org.apache.seatunnel.common.config.Common;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.common.utils.FileUtils;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.plugin.discovery.PluginIdentifier;
import org.apache.seatunnel.plugin.discovery.seatunnel.SeaTunnelSinkPluginDiscovery;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TableSchemaServiceImpl extends SeatunnelBaseServiceImpl
        implements ITableSchemaService {

    @Resource private ConnectorCache connectorCache;

    @Resource private ConnectorDataSourceMapperConfig connectorDataSourceMapperConfig;

    @Resource(name = "datasourceServiceImpl")
    private IDatasourceService dataSourceService;

    private final DataTypeConvertorFactory factory;

    public TableSchemaServiceImpl() throws IOException {
        Common.setStarter(true);
        Path path = new SeaTunnelSinkPluginDiscovery().getPluginDir();
        if (path.toFile().exists()) {
            List<URL> files = FileUtils.searchJarFiles(path);
            files.addAll(FileUtils.searchJarFiles(Common.pluginRootDir()));
            factory = new DataTypeConvertorFactory(new URLClassLoader(files.toArray(new URL[0])));
        } else {
            factory = new DataTypeConvertorFactory();
        }
    }

    @Override
    public TableSchemaRes getSeaTunnelSchema(String pluginName, TableSchemaReq tableSchemaReq) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.JOB_TABLE_SCHEMA, 0);
        pluginName = pluginName.toUpperCase();
        if (pluginName.endsWith("-CDC")) {
            pluginName = pluginName.replace("-CDC", "");
        } else if (pluginName.startsWith("JDBC_")) {
            pluginName = pluginName.replace("JDBC_", "");
        } else if (pluginName.startsWith("JDBC-")) {
            pluginName = pluginName.replace("JDBC-", "");
        }
        DataTypeConvertor<?> convertor = factory.getDataTypeConvertor(pluginName);

        for (TableField field : tableSchemaReq.getFields()) {
            SeaTunnelDataType<?> dataType = convertor.toSeaTunnelType(field.getType());
            field.setType(dataType.toString());
        }
        TableSchemaRes res = new TableSchemaRes();
        res.setFields(tableSchemaReq.getFields());
        return res;
    }

    @Override
    public void getAddSeaTunnelSchema(List<TableField> tableFields, String pluginName) {
        pluginName = pluginName.toUpperCase();
        if (pluginName.endsWith("-CDC")) {
            pluginName = pluginName.replace("-CDC", "");
        } else if (pluginName.startsWith("JDBC_")) {
            pluginName = pluginName.replace("JDBC_", "");
        } else if (pluginName.startsWith("JDBC-")) {
            pluginName = pluginName.replace("JDBC-", "");
        }
        DataTypeConvertor<?> convertor = factory.getDataTypeConvertor(pluginName);
        for (TableField field : tableFields) {
            try {
                SeaTunnelDataType<?> dataType = convertor.toSeaTunnelType(field.getType());
                field.setUnSupport(false);
                field.setOutputDataType(dataType.toString());
            } catch (Exception exception) {
                field.setUnSupport(true);
                log.warn(
                        "Database {} , field {} is unSupport",
                        pluginName,
                        field.getType(),
                        exception);
            }
        }
    }

    @Override
    public boolean getColumnProjection(String pluginName) {
        funcPermissionCheck(SeatunnelFuncPermissionKeyConstant.JOB_TABLE_COLUMN_PROJECTION, 0);
        String connector =
                connectorDataSourceMapperConfig
                        .findConnectorForDatasourceName(pluginName)
                        .orElseThrow(
                                () ->
                                        new SeatunnelException(
                                                SeatunnelErrorEnum.ILLEGAL_STATE,
                                                "Unsupported Data Source Name"));
        return connectorCache
                .getConnectorFeature(
                        PluginIdentifier.of("seatunnel", PluginType.SOURCE.getType(), connector))
                .isSupportColumnProjection();
    }

    @Override
    public DataSourceOption checkDatabaseAndTable(
            String datasourceId, DataSourceOption dataSourceOption) {
        List<String> notExistDatabases = new ArrayList<>();
        String datasourceName =
                dataSourceService.queryDatasourceDetailById(datasourceId).getDatasourceName();
        if (dataSourceOption.getDatabases() != null) {
            List<String> databases =
                    dataSourceService.queryDatabaseByDatasourceName(datasourceName);
            notExistDatabases.addAll(
                    dataSourceOption.getDatabases().stream()
                            .filter(database -> !databases.contains(database))
                            .collect(Collectors.toList()));
        }
        Map<String, Set<String>> tables = new HashMap<>();
        if (dataSourceOption.getTables() != null) {
            List<String> notExistTables = new ArrayList<>();
            dataSourceOption
                    .getTables()
                    .forEach(
                            tableStr -> {
                                String database;
                                String table;
                                //                                if (tableStr.contains(".")) {
                                //                                    String[] split =
                                // tableStr.split("\\.");
                                //                                    database = split[0];
                                //                                    table = split[1];
                                //                                } else {
                                database = dataSourceOption.getDatabases().get(0);
                                table = tableStr;
                                //                                }
                                if (!tables.containsKey(database)) {
                                    if (notExistDatabases.contains(database)) {
                                        notExistTables.add(tableStr);
                                        return;
                                    } else {
                                        tables.put(
                                                database,
                                                new HashSet<>(
                                                        dataSourceService.queryTableNames(
                                                                datasourceName, database)));
                                    }
                                }
                                if (!tables.get(database).contains(table)) {
                                    notExistTables.add(tableStr);
                                }
                            });
            return new DataSourceOption(notExistDatabases, notExistTables);
        }
        return new DataSourceOption(notExistDatabases, new ArrayList<>());
    }
}
