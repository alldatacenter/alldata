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

package org.apache.seatunnel.datasource;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.common.utils.ExceptionUtils;
import org.apache.seatunnel.datasource.classloader.DatasourceClassLoader;
import org.apache.seatunnel.datasource.classloader.DatasourceLoadConfig;
import org.apache.seatunnel.datasource.exception.DataSourceSDKException;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourceFactory;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginInfo;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;
import org.apache.seatunnel.datasource.service.DataSourceService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class AbstractDataSourceClient implements DataSourceService {
    private static final String ST_WEB_BASEDIR_PATH = "ST_WEB_BASEDIR_PATH";
    //    private ClassLoader datasourceClassLoader; // thradlocal
    private ThreadLocal<ClassLoader> datasourceClassLoader = new ThreadLocal<>();

    private Map<String, DataSourcePluginInfo> supportedDataSourceInfo = new HashMap<>();

    private Map<String, Integer> supportedDataSourceIndex = new HashMap<>();

    protected List<DataSourcePluginInfo> supportedDataSources = new ArrayList<>();

    private List<DataSourceChannel> dataSourceChannels = new ArrayList<>();

    private Map<String, DataSourceChannel> classLoaderChannel = new HashMap<>();

    protected AbstractDataSourceClient() {
        AtomicInteger dataSourceIndex = new AtomicInteger();
        for (String pluginName : DatasourceLoadConfig.pluginSet) {
            log.info("plugin set : " + pluginName);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getCustomClassloader(pluginName));
            if (DatasourceLoadConfig.classLoaderChannel.get(pluginName.toUpperCase()) != null) {
                log.info(pluginName + " is exist");
                continue;
            }
            try {
                Class<?> clazz =
                        Class.forName(
                                DatasourceLoadConfig.classLoaderFactoryName.get(
                                        pluginName.toUpperCase()),
                                true,
                                Thread.currentThread().getContextClassLoader());
                DataSourceFactory factory =
                        (DataSourceFactory) clazz.getDeclaredConstructor().newInstance();
                log.info("factory : " + String.valueOf(factory));
                Set<DataSourcePluginInfo> dataSourcePluginInfos = factory.supportedDataSources();
                dataSourcePluginInfos.forEach(
                        dataSourceInfo -> {
                            supportedDataSourceInfo.put(
                                    dataSourceInfo.getName().toUpperCase(), dataSourceInfo);
                            supportedDataSourceIndex.put(
                                    dataSourceInfo.getName().toUpperCase(), dataSourceIndex.get());
                            supportedDataSources.add(dataSourceInfo);
                            log.info("factory : " + dataSourceInfo);
                        });
                DatasourceLoadConfig.classLoaderChannel.put(
                        pluginName.toUpperCase(), factory.createChannel());
                log.info(
                        DatasourceLoadConfig.classLoaderChannel
                                .get(pluginName.toUpperCase())
                                .toString());
            } catch (Exception e) {
                log.warn("datasource " + pluginName + "is error" + ExceptionUtils.getMessage(e));
            }
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
        if (supportedDataSourceInfo.isEmpty()) {
            throw new DataSourceSDKException("No supported data source found");
        }
    }

    @Override
    public Boolean checkDataSourceConnectivity(
            String pluginName, Map<String, String> dataSourceParams) {
        updateClassLoader(pluginName);
        boolean isConnect =
                getDataSourceChannel(pluginName)
                        .checkDataSourceConnectivity(pluginName, dataSourceParams);
        classLoaderRestore();
        return isConnect;
    }

    @Override
    public List<DataSourcePluginInfo> listAllDataSources() {
        return supportedDataSources;
    }

    protected DataSourceChannel getDataSourceChannel(String pluginName) {
        checkNotNull(pluginName, "pluginName cannot be null");
        //        Integer index = supportedDataSourceIndex.get(pluginName.toUpperCase());
        //        if (index == null) {
        //            throw new DataSourceSDKException(
        //                    "The %s plugin is not supported or plugin not exist.", pluginName);
        //        }
        return DatasourceLoadConfig.classLoaderChannel.get(pluginName.toUpperCase());
    }

    @Override
    public OptionRule queryDataSourceFieldByName(String pluginName) {
        updateClassLoader(pluginName);
        OptionRule dataSourceOptions =
                getDataSourceChannel(pluginName).getDataSourceOptions(pluginName);
        classLoaderRestore();
        return dataSourceOptions;
    }

    @Override
    public OptionRule queryMetadataFieldByName(String pluginName) {
        updateClassLoader(pluginName);
        OptionRule datasourceMetadataFieldsByDataSourceName =
                getDataSourceChannel(pluginName)
                        .getDatasourceMetadataFieldsByDataSourceName(pluginName);
        classLoaderRestore();
        return datasourceMetadataFieldsByDataSourceName;
    }

    @Override
    public List<String> getTables(
            String pluginName,
            String databaseName,
            Map<String, String> requestParams,
            Map<String, String> options) {
        updateClassLoader(pluginName);
        List<String> tables =
                getDataSourceChannel(pluginName)
                        .getTables(pluginName, requestParams, databaseName, options);
        classLoaderRestore();
        return tables;
    }

    @Override
    public List<String> getDatabases(String pluginName, Map<String, String> requestParams) {
        updateClassLoader(pluginName);
        List<String> databases =
                getDataSourceChannel(pluginName).getDatabases(pluginName, requestParams);
        classLoaderRestore();
        return databases;
    }

    @Override
    public List<TableField> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            String tableName) {
        updateClassLoader(pluginName);
        List<TableField> tableFields =
                getDataSourceChannel(pluginName)
                        .getTableFields(pluginName, requestParams, databaseName, tableName);
        classLoaderRestore();
        return tableFields;
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            List<String> tableNames) {
        updateClassLoader(pluginName);
        Map<String, List<TableField>> tableFields =
                getDataSourceChannel(pluginName)
                        .getTableFields(pluginName, requestParams, databaseName, tableNames);
        classLoaderRestore();
        return tableFields;
    }

    @Override
    public Pair<String, String> getTableSyncMaxValue(
            String pluginName,
            Map<String, String> requestParams,
            String databaseName,
            String tableName,
            String updateFieldType) {
        updateClassLoader(pluginName);
        Pair<String, String> tableSyncMaxValue =
                getDataSourceChannel(pluginName)
                        .getTableSyncMaxValue(
                                pluginName,
                                requestParams,
                                databaseName,
                                tableName,
                                updateFieldType);
        classLoaderRestore();
        return tableSyncMaxValue;
    }

    private ClassLoader getCustomClassloader(String pluginName) {
        String getenv = System.getenv(ST_WEB_BASEDIR_PATH);
        log.info("ST_WEB_BASEDIR_PATH is : " + getenv);
        String libPath = StringUtils.isEmpty(getenv) ? "/datasource" : (getenv + "/datasource");

        //        String libPath = "/root/apache-seatunnel-web-2.4.7-WS-SNAPSHOT/datasource/";
        File jarDirectory = new File(libPath);
        File[] jarFiles =
                jarDirectory.listFiles(
                        (dir, name) -> {
                            String pluginUpperCase = pluginName.toUpperCase();
                            String nameLowerCase = name.toLowerCase();
                            if (pluginUpperCase.equals("KAFKA")) {
                                return !nameLowerCase.contains("kingbase")
                                        && nameLowerCase.startsWith(
                                                DatasourceLoadConfig.classLoaderJarName.get(
                                                        pluginUpperCase));
                            } else {
                                return nameLowerCase.startsWith(
                                        DatasourceLoadConfig.classLoaderJarName.get(
                                                pluginUpperCase));
                            }
                        });

        log.info("jar file length :" + (jarFiles == null ? 0 : jarFiles.length));
        log.info("jar file length :" + (jarFiles == null ? 0 : jarFiles[0].getName()));
        DatasourceClassLoader customClassLoader =
                DatasourceLoadConfig.datasourceClassLoaders.get(pluginName.toUpperCase());
        try {
            if (customClassLoader == null) {
                jarFiles = jarFiles == null ? new File[0] : jarFiles;
                URL[] urls = new URL[jarFiles.length];
                for (int i = 0; i < jarFiles.length; i++) {
                    try {
                        urls[i] = jarFiles[i].toURI().toURL();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                customClassLoader =
                        new DatasourceClassLoader(
                                urls, Thread.currentThread().getContextClassLoader());
                DatasourceLoadConfig.datasourceClassLoaders.put(
                        pluginName.toUpperCase(), customClassLoader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("custom loader is:" + String.valueOf(customClassLoader));
        return customClassLoader;
    }

    private void updateClassLoader(String pluginName) {
        log.info("update class loader");
        datasourceClassLoader.set(Thread.currentThread().getContextClassLoader());
        ClassLoader customClassLoader = getCustomClassloader(pluginName);
        Thread.currentThread().setContextClassLoader(customClassLoader);
        log.info(customClassLoader.toString());
    }

    private void classLoaderRestore() {
        try {
            log.info("close class loader");
            Thread.currentThread().setContextClassLoader(datasourceClassLoader.get());
        } catch (Exception e) {
            log.info("loader catch");
        }
    }

    @Override
    public Connection getConnection(String pluginName, Map<String, String> requestParams) {
        updateClassLoader(pluginName);
        Connection connection =
                getDataSourceChannel(pluginName).getConnection(pluginName, requestParams);
        classLoaderRestore();
        return connection;
    }
}
