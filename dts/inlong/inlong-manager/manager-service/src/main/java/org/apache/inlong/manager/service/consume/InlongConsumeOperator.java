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

package org.apache.inlong.manager.service.consume;

import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;

/**
 * Interface of the inlong consume operator.
 */
public interface InlongConsumeOperator {

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
     * Check whether the topic in inlong consume belongs to its inlong group id.
     *
     * @param request inlong consume request
     */
    void checkTopicInfo(InlongConsumeRequest request);

    /**
     * Save the inlong consume info.
     *
     * @param request request of the group
     * @param operator name of the operator
     * @return inlong consume id after saving
     */
    Integer saveOpt(InlongConsumeRequest request, String operator);

    /**
     * Get the inlong consume info from the given entity.
     *
     * @param entity get field value from the entity
     * @return inlong consume info after encapsulating
     */
    InlongConsumeInfo getFromEntity(InlongConsumeEntity entity);

    /**
     * Update the inlong consume info.
     *
     * @param request request of update
     * @param operator name of operator
     */
    void updateOpt(InlongConsumeRequest request, String operator);

}
