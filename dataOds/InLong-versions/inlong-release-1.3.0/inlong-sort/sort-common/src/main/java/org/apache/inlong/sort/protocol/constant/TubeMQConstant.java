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
 *
 */

package org.apache.inlong.sort.protocol.constant;

/**
 * TubeMQ option constant.
 */
public class TubeMQConstant {

    public static final String TOPIC = "topic";

    public static final String GROUP_ID = "group.id";

    public static final String CONNECTOR = "connector";

    public static final String TUBEMQ = "tubemq";

    public static final String MASTER_RPC = "master.rpc";

    public static final String FORMAT = "format";

    public static final String SESSION_KEY = "session.key";

    /**
     * The tubemq consumers use this tid set to filter records reading from server.
     */
    public static final String TID = "tid";

    public static final String CONSUMER_STARTUP_MODE = "consumer.startup.mode";

}
