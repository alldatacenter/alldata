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

package org.apache.seatunnel.datasource.plugin.s3;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.datasource.plugin.api.DataSourceChannel;
import org.apache.seatunnel.datasource.plugin.api.DataSourcePluginException;
import org.apache.seatunnel.datasource.plugin.api.model.TableField;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class S3DatasourceChannel implements DataSourceChannel {
    @Override
    public OptionRule getDataSourceOptions(@NonNull String pluginName) {
        return S3OptionRule.optionRule();
    }

    @Override
    public OptionRule getDatasourceMetadataFieldsByDataSourceName(@NonNull String pluginName) {
        return S3OptionRule.metadataRule();
    }

    @Override
    public List<String> getTables(
            @NonNull String pluginName,
            Map<String, String> requestParams,
            String database,
            Map<String, String> options) {
        throw new UnsupportedOperationException("getTables is not supported for S3 datasource");
    }

    @Override
    public List<String> getDatabases(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        throw new UnsupportedOperationException("getDatabases is not supported for S3 datasource");
    }

    @Override
    public boolean checkDataSourceConnectivity(
            @NonNull String pluginName, @NonNull Map<String, String> requestParams) {
        Configuration conf = HadoopS3AConfiguration.getConfiguration(requestParams);
        try (FileSystem fs = FileSystem.get(conf)) {
            fs.listStatus(new Path("/"));
            return true;
        } catch (IOException e) {
            throw new DataSourcePluginException(
                    String.format("check s3 connectivity failed, config is: %s", requestParams), e);
        }
    }

    @Override
    public List<TableField> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull String table) {
        throw new UnsupportedOperationException(
                "getTableFields is not supported for S3 datasource");
    }

    @Override
    public Map<String, List<TableField>> getTableFields(
            @NonNull String pluginName,
            @NonNull Map<String, String> requestParams,
            @NonNull String database,
            @NonNull List<String> tables) {
        throw new UnsupportedOperationException(
                "getTableFields is not supported for S3 datasource");
    }
}
