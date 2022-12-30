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

package org.apache.inlong.manager.service.resource.queue;

import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Interface of the message queue resource operator
 */
public interface QueueResourceOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    boolean accept(String mqType);

    /**
     * Create message queue resource for Inlong Group.
     *
     * @param groupInfo inlong group info
     * @param operator operator name
     */
    default void createQueueForGroup(@NotNull InlongGroupInfo groupInfo, @NotBlank String operator) {
    }

    /**
     * Delete message queue resource for Inlong Group.
     *
     * @param groupInfo inlong group info
     * @param operator operator name
     */
    default void deleteQueueForGroup(InlongGroupInfo groupInfo, String operator) {
    }

    /**
     * Create message queue resource for Inlong Stream.
     *
     * @param groupInfo inlong group info
     * @param streamInfo inlong stream info
     * @param operator operator name
     */
    default void createQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
    }

    /**
     * Delete message queue resource for Inlong Stream.
     *
     * @param groupInfo inlong group info
     * @param streamInfo inlong stream info
     * @param operator operator name
     */
    default void deleteQueueForStream(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo, String operator) {
    }

}
