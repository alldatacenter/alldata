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
package io.datavines.registry.plugin;


import io.datavines.registry.api.ConnectionListener;
import io.datavines.registry.api.ConnectionStatus;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperConnectionStateListener implements ConnectionStateListener {

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperConnectionStateListener.class);

    private final ConnectionListener listener;

    public ZookeeperConnectionStateListener(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState newState) {

        switch (newState) {
            case LOST:
                logger.warn("Registry disconnected");
                listener.onUpdate(ConnectionStatus.DISCONNECTED);
                break;
            case RECONNECTED:
                logger.warn("Registry reconnected");
                listener.onUpdate(ConnectionStatus.RECONNECTED);
                break;
            default:
                break;
        }
    }
}