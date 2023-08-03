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

package org.apache.seatunnel.app.service;

import org.apache.seatunnel.app.domain.request.connector.ConnectorStatus;
import org.apache.seatunnel.app.domain.request.connector.SceneMode;
import org.apache.seatunnel.app.domain.response.connector.ConnectorInfo;
import org.apache.seatunnel.app.domain.response.connector.DataSourceInstance;
import org.apache.seatunnel.app.dynamicforms.FormStructure;

import lombok.NonNull;

import java.io.IOException;
import java.util.List;

public interface IConnectorService {

    List<ConnectorInfo> listSources(ConnectorStatus status);

    List<DataSourceInstance> listSourceDataSourceInstances(
            Long jobId, SceneMode sceneMode, ConnectorStatus status);

    List<ConnectorInfo> listTransforms();

    List<ConnectorInfo> listTransformsForJob(Long jobId);

    List<ConnectorInfo> listSinks(ConnectorStatus status);

    List<DataSourceInstance> listSinkDataSourcesInstances(Long jobId, ConnectorStatus status);

    void sync() throws IOException;

    FormStructure getConnectorFormStructure(
            @NonNull String pluginType, @NonNull String connectorName);

    FormStructure getTransformFormStructure(
            @NonNull String pluginType, @NonNull String connectorName);

    FormStructure getDatasourceFormStructure(
            @NonNull Long jobId, @NonNull Long dataSourceId, @NonNull String pluginType);
}
