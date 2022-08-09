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

package org.apache.ambari.funtest.server;

/**
 * Ambari server connection parameters
 */
public class ConnectionParams {
    /**
     * Server name
     */
    private String serverName;

    /**
     * Server port
     */
    private int serverApiPort;

    /**
     * Agent server port
     */
    private int serverAgentPort;

    /**
     * User name
     */
    private String userName;

    /**
     * Password
     */
    private String password;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public int getServerApiPort() {
        return serverApiPort;
    }

    public void setServerApiPort(int serverApiPort) {
        this.serverApiPort = serverApiPort;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getServerAgentPort() {
        return serverAgentPort;
    }

    public void setServerAgentPort(int serverAgentPort) {
        this.serverAgentPort = serverAgentPort;
    }
}
