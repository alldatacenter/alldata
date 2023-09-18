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
package io.datavines.registry.api;

import io.datavines.spi.SPI;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

@SPI
public interface Registry {

    void init(Properties properties) throws Exception;

    boolean acquire(String key, long timeout);

    boolean release(String key);

    void subscribe(String key, SubscribeListener listener);

    void unSubscribe(String key);

    void addConnectionListener(ConnectionListener connectionListener);

    List<ServerInfo> getActiveServerList();

    void close() throws SQLException;
}
