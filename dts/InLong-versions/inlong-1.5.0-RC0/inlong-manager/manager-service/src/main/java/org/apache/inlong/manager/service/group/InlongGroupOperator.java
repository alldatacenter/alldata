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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;

/**
 * Interface of the inlong group operator.
 */
public interface InlongGroupOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String mqType);

    /**
     * Get the MQ type.
     *
     * @return MQ type string
     */
    String getMQType();

    /**
     * Save the inlong group info.
     *
     * @param request request of the group
     * @param operator name of the operator
     * @return group id after saving
     */
    String saveOpt(InlongGroupRequest request, String operator);

    /**
     * Get the group info from the given entity.
     *
     * @param entity get field value from the entity
     * @return group info after encapsulating
     */
    InlongGroupInfo getFromEntity(InlongGroupEntity entity);

    /**
     * Update the inlong group info.
     *
     * @param request request of update
     * @param operator name of operator
     */
    void updateOpt(InlongGroupRequest request, String operator);

    /**
     * Get the topic info for the given inlong group.
     *
     * @param groupInfo inlong group which need to get topic info
     * @return topic info
     */
    InlongGroupTopicInfo getTopic(InlongGroupInfo groupInfo);

    /**
     * Get backup topic info for the given inlong group if exists.
     *
     * @param groupInfo inlong group info
     * @return backup topic info
     */
    default InlongGroupTopicInfo getBackupTopic(InlongGroupInfo groupInfo) {
        return null;
    }

}
