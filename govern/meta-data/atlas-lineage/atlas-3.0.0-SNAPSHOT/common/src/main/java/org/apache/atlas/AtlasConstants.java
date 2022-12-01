/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

/**
 * Constants used in Atlas configuration.
 */
public final class AtlasConstants {
    private AtlasConstants() {
    }

    public static final String CLUSTER_NAME_KEY              = "atlas.cluster.name";
    public static final String METADATA_NAMESPACE_KEY        = "atlas.metadata.namespace";
    public static final String DEFAULT_CLUSTER_NAME          = "primary";
    public static final String SYSTEM_PROPERTY_APP_PORT      = "atlas.app.port";
    public static final String ATLAS_REST_ADDRESS_KEY        = "atlas.rest.address";
    public static final String ATLAS_MIGRATION_MODE_FILENAME = "atlas.migration.data.filename";
    public static final String ATLAS_SERVICES_ENABLED        = "atlas.services.enabled";

    public static final String CLUSTER_NAME_ATTRIBUTE        = "clusterName";
    public static final String DEFAULT_APP_PORT_STR          = "21000";
    public static final String DEFAULT_ATLAS_REST_ADDRESS    = "http://localhost:21000";
    public static final String DEFAULT_TYPE_VERSION          = "1.0";
    public static final int    ATLAS_SHUTDOWN_HOOK_PRIORITY  = 30;
}