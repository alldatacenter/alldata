/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class HdfsNameServiceResolver {
    private static final Logger LOG = LoggerFactory.getLogger(HdfsNameServiceResolver.class);

    public static final String HDFS_SCHEME                            = "hdfs://";

    private static final int    DEFAULT_PORT                           = 8020;
    private static final String HDFS_NAMESERVICE_PROPERTY_KEY          = "dfs.nameservices";
    private static final String HDFS_INTERNAL_NAMESERVICE_PROPERTY_KEY = "dfs.internal.nameservices";
    private static final String HDFS_NAMENODES_HA_NODES_PREFIX         = "dfs.ha.namenodes.";
    private static final String HDFS_NAMENODE_HA_ADDRESS_TEMPLATE      = "dfs.namenode.rpc-address.%s.%s";
    private static final String HDFS_NAMENODE_ADDRESS_TEMPLATE         = "dfs.namenode.rpc-address.%s";

    private static final Map<String, String> hostToNameServiceMap = new HashMap<>();

    static {
        init();
    }

    public static String getPathWithNameServiceID(String path) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HdfsNameServiceResolver.getPathWithNameServiceID({})", path);
        }

        String ret = path;

        // Only handle URLs that begin with hdfs://
        if (path != null && path.indexOf(HDFS_SCHEME) == 0) {
            URI uri = new Path(path).toUri();
            String nsId;

            if (uri.getPort() != -1) {
                nsId = hostToNameServiceMap.get(uri.getAuthority());
            } else {
                nsId = hostToNameServiceMap.get(uri.getHost() + ":" + DEFAULT_PORT);
            }

            if (nsId != null) {
                ret = StringUtils.replaceOnce(path, uri.getAuthority(), nsId);
            }
        }


        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HdfsNameServiceResolver.getPathWithNameServiceID({}) = {}", path, ret);
        }

        return ret;
    }

    public static String getNameServiceIDForPath(String path) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HdfsNameServiceResolver.getNameServiceIDForPath({})", path);
        }

        String ret = "";

        // Only handle path URLs that begin with hdfs://
        if (path != null && path.indexOf(HDFS_SCHEME) == 0) {
            try {
                URI uri = new Path(path).toUri();

                if (uri != null) {
                    // URI can contain host and port
                    if (uri.getPort() != -1) {
                        ret = getNameServiceID(uri.getHost(), uri.getPort());
                    } else {
                        // No port information present, it means the path might contain only host or the nameservice id itself
                        // Try resolving using default port
                        ret = getNameServiceID(uri.getHost(), DEFAULT_PORT);

                        // If not resolved yet, then the path must contain nameServiceId
                        if (StringUtils.isEmpty(ret) && hostToNameServiceMap.containsValue(uri.getHost())) {
                            ret = uri.getHost();
                        }
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // No need to do anything
            }
        }


        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HdfsNameServiceResolver.getNameServiceIDForPath({}) = {}", path, ret);
        }

        return ret;
    }

    private static String getNameServiceID(String host, int port) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HdfsNameServiceResolver.getNameServiceID({}, {})", host, port);
        }

        String ret = hostToNameServiceMap.getOrDefault(host + ":" + port, "");

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HdfsNameServiceResolver.getNameServiceID({}, {}) = {}", host, port, ret);
        }

        return ret;
    }

    private static void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HdfsNameServiceResolver.init()");
        }
        HdfsConfiguration hdfsConfiguration = new HdfsConfiguration(true);

        // Determine all available nameServiceIDs
        String[] nameServiceIDs = hdfsConfiguration.getTrimmedStrings(HDFS_INTERNAL_NAMESERVICE_PROPERTY_KEY);
        if (Objects.isNull(nameServiceIDs) || nameServiceIDs.length == 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("NSID not found for {}, looking under {}", HDFS_INTERNAL_NAMESERVICE_PROPERTY_KEY, HDFS_NAMESERVICE_PROPERTY_KEY);
            }
            // Attempt another lookup using internal name service IDs key
            nameServiceIDs = hdfsConfiguration.getTrimmedStrings(HDFS_NAMESERVICE_PROPERTY_KEY);
        }

        if (Objects.nonNull(nameServiceIDs) && nameServiceIDs.length > 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("NSIDs = {}", nameServiceIDs);
            }

            boolean isHA;
            for (String nameServiceID : nameServiceIDs) {
                // Find NameNode addresses and map to the NameServiceID
                String[] nameNodes = hdfsConfiguration.getTrimmedStrings(HDFS_NAMENODES_HA_NODES_PREFIX + nameServiceID);
                isHA = nameNodes != null && nameNodes.length > 0;

                String nameNodeMappingKey, nameNodeAddress;
                if (isHA) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Processing HA node info");
                    }

                    for (String nameNode : nameNodes) {
                        nameNodeMappingKey = String.format(HDFS_NAMENODE_HA_ADDRESS_TEMPLATE, nameServiceID, nameNode);
                        nameNodeAddress = hdfsConfiguration.get(nameNodeMappingKey, "");

                        // Add a mapping only if found
                        if (StringUtils.isNotEmpty(nameNodeAddress)) {
                            hostToNameServiceMap.put(nameNodeAddress, nameServiceID);
                        }
                    }
                } else {
                    nameNodeMappingKey = String.format(HDFS_NAMENODE_ADDRESS_TEMPLATE, nameServiceID);
                    nameNodeAddress = hdfsConfiguration.get(nameNodeMappingKey, "");

                    // Add a mapping only if found
                    if (StringUtils.isNotEmpty(nameNodeAddress)) {
                        hostToNameServiceMap.put(nameNodeAddress, nameServiceID);
                    }
                }
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No NSID could be resolved");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== HdfsNameServiceResolver.init()");
        }
    }
}
