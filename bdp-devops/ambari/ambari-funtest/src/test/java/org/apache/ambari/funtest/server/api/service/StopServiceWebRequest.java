/*
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

package org.apache.ambari.funtest.server.api.service;

import org.apache.ambari.funtest.server.ConnectionParams;
import org.apache.ambari.server.state.State;

/**
 * Stop a service by updating it's state to INSTALLED.
 */
public class StopServiceWebRequest extends SetServiceStateWebRequest {
    /**
     * Updates the state of the specified service to INSTALLED.
     *
     * @param params - Ambari server connection information.
     * @param clusterName - Existing cluster name.
     * @param serviceName - Service to be stopped.
     */
    public StopServiceWebRequest(ConnectionParams params, String clusterName, String serviceName) {
        super(params, clusterName, serviceName, State.INSTALLED, "Stop service");
    }
}
