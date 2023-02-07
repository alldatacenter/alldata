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

package org.apache.inlong.sort.protocol;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * The concept of Groupinfo is the same as that of inlong group,
 * and it is the smallest unit of sort task execution.
 */
@Data
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 6034630524669634079L;

    @JsonProperty("groupId")
    private String groupId;
    @JsonProperty("streams")
    private List<StreamInfo> streams;

    private Map<String, String> properties;

    /**
     * Information of group.
     * 
     * @param groupId Uniquely identifies of GroupInfo
     * @param streams The StreamInfo list that GroupInfo contains
     */
    @JsonCreator
    public GroupInfo(@JsonProperty("groupId") String groupId,
            @JsonProperty("streams") List<StreamInfo> streams) {
        this.groupId = Preconditions.checkNotNull(groupId, "groupId is null");
        this.streams = Preconditions.checkNotNull(streams, "streams is null");
        this.properties = new HashMap<>();
        Preconditions.checkState(!streams.isEmpty(), "streams is empty");
    }

    public GroupInfo(@JsonProperty("groupId") String groupId,
            @JsonProperty("streams") List<StreamInfo> streams,
            Map<String, String> properties) {
        this.groupId = Preconditions.checkNotNull(groupId, "groupId is null");
        this.streams = Preconditions.checkNotNull(streams, "streams is null");
        this.properties = properties;
        Preconditions.checkState(!streams.isEmpty(), "streams is empty");
    }
}
