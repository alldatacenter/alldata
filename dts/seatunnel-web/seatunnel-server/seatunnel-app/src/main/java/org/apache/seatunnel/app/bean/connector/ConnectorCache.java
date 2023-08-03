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

package org.apache.seatunnel.app.bean.connector;

import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.app.domain.response.connector.ConnectorFeature;
import org.apache.seatunnel.app.domain.response.connector.ConnectorInfo;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.thirdparty.framework.PluginDiscoveryUtil;
import org.apache.seatunnel.common.config.Common;
import org.apache.seatunnel.common.config.DeployMode;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.plugin.discovery.PluginIdentifier;
import org.apache.seatunnel.server.common.SeatunnelErrorEnum;
import org.apache.seatunnel.server.common.SeatunnelException;

import org.springframework.stereotype.Component;

import lombok.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectorCache {

    private final ConcurrentMap<PluginType, List<ConnectorInfo>> downloadConnectorCache =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<PluginType, List<ConnectorInfo>> allConnectorCache =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<PluginIdentifier, OptionRule> allConnectorOptionRule =
            new ConcurrentHashMap<>();

    private List<ConnectorInfo> transformCache = new CopyOnWriteArrayList<>();

    private ConcurrentMap<String, FormStructure> sourceFormStructureCache =
            new ConcurrentHashMap<>();

    private ConcurrentMap<String, FormStructure> sinkFormStructureCache = new ConcurrentHashMap<>();

    private ConcurrentMap<String, FormStructure> transformFormStructureCache =
            new ConcurrentHashMap<>();

    private Map<PluginIdentifier, ConnectorFeature> featureMap = new HashMap<>();

    public ConnectorCache() throws IOException {
        refresh();
    }

    public List<ConnectorInfo> getAllConnectors(PluginType pluginType) {
        return allConnectorCache.get(pluginType);
    }

    public List<ConnectorInfo> getTransform() {
        return transformCache;
    }

    public List<ConnectorInfo> getDownLoadConnector(PluginType pluginType) {
        return downloadConnectorCache.get(pluginType);
    }

    public List<ConnectorInfo> getNotDownLoadConnector(PluginType pluginType) {
        Map<PluginIdentifier, ConnectorInfo> allConnectors =
                allConnectorCache.get(pluginType).stream()
                        .collect(
                                Collectors.toMap(
                                        ConnectorInfo::getPluginIdentifier, Function.identity()));
        downloadConnectorCache
                .get(pluginType)
                .forEach(d -> allConnectors.remove(d.getPluginIdentifier()));
        return new ArrayList<>(allConnectors.values());
    }

    public ConnectorFeature getConnectorFeature(PluginIdentifier connectorInfo) {
        return featureMap.get(connectorInfo);
    }

    public synchronized void refresh() throws IOException {
        Common.setDeployMode(DeployMode.CLIENT);
        Map<PluginType, LinkedHashMap<PluginIdentifier, OptionRule>> allConnectors =
                PluginDiscoveryUtil.getAllConnectors();
        allConnectorOptionRule.clear();
        allConnectors.forEach((key, value) -> allConnectorOptionRule.putAll(value));

        downloadConnectorCache.put(
                PluginType.SOURCE,
                PluginDiscoveryUtil.getDownloadedConnectors(allConnectors, PluginType.SOURCE));
        downloadConnectorCache.put(
                PluginType.SINK,
                PluginDiscoveryUtil.getDownloadedConnectors(allConnectors, PluginType.SINK));
        allConnectorCache.put(
                PluginType.SOURCE,
                PluginDiscoveryUtil.getAllConnectorsFromPluginMapping(PluginType.SOURCE));
        allConnectorCache.put(
                PluginType.SINK,
                PluginDiscoveryUtil.getAllConnectorsFromPluginMapping(PluginType.SINK));
        transformCache = PluginDiscoveryUtil.getTransforms(allConnectors);

        sourceFormStructureCache =
                PluginDiscoveryUtil.getDownloadedConnectorFormStructures(
                        allConnectors, PluginType.SOURCE);
        sinkFormStructureCache =
                PluginDiscoveryUtil.getDownloadedConnectorFormStructures(
                        allConnectors, PluginType.SINK);
        transformFormStructureCache = PluginDiscoveryUtil.getTransformFormStructures(allConnectors);
        syncSourceFeature();
    }

    private void syncSourceFeature() throws IOException {
        featureMap = PluginDiscoveryUtil.getConnectorFeatures(PluginType.SOURCE);
    }

    public FormStructure getFormStructure(
            @NonNull String pluginType, @NonNull String connectorName) {
        if (PluginType.SOURCE.getType().equals(pluginType)) {
            return sourceFormStructureCache.get(connectorName);
        }

        if (PluginType.TRANSFORM.getType().equals(pluginType)) {
            return transformFormStructureCache.get(connectorName);
        }

        if (PluginType.SINK.getType().equals(pluginType)) {
            return sinkFormStructureCache.get(connectorName);
        }

        throw new SeatunnelException(SeatunnelErrorEnum.UNSUPPORTED_CONNECTOR_TYPE, pluginType);
    }

    public OptionRule getOptionRule(@NonNull String pluginType, @NonNull String connectorName) {
        return allConnectorOptionRule.get(
                PluginIdentifier.of("seatunnel", pluginType, connectorName));
    }
}
