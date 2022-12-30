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

package org.apache.inlong.agent.pojo;

import lombok.Data;

@Data
public class KafkaJob {

    private String topic;
    private String bootstrapServers;
    private Group group;
    private Bootstrap bootstrap;
    private Partition partition;
    private RecordSpeed recordSpeed;
    private ByteSpeed byteSpeed;
    private String autoOffsetReset;

    @Data
    public static class Group {
        private String id;
    }

    @Data
    public static class Bootstrap {
        private String servers;
    }

    @Data
    public static class Partition {
        private String offset;
    }

    @Data
    public static class RecordSpeed {
        private String limit;
    }

    @Data
    public static class ByteSpeed {
        private String limit;
    }

    @Data
    public static class KafkaJobTaskConfig {

        private String topic;
        private String bootstrapServers;
        private String groupId;
        private String recordSpeedLimit;
        private String byteSpeedLimit;
        private  String autoOffsetReset;
    }
}
