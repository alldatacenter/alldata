/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.admin;

import javax.management.MXBean;

/**
 * ProxyServiceMBean<br>
 * Stop TCP service before stopping DataProxy process.<br>
 * TCP service will response error to agent, agent will resend data to other DataProxy.<br>
 * After DataProxy send all channel data to cache cluster, DataProxy can be stopped.<br>
 */
@MXBean
public interface ProxyServiceMBean {

    String MBEAN_TYPE = "ProxyService";
    String METHOD_STOPSERVICE = "stopService";
    String KEY_SOURCENAME = "sourceName";
    String ALL_SOURCENAME = "*";
    String METHOD_RECOVERSERVICE = "recoverService";

    /**
     * stopService
     */
    void stopService();

    /**
     * recoverService
     */
    void recoverService();
}
