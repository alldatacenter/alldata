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

package org.apache.inlong.manager.service.core;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consumption.ConsumptionInfo;
import org.apache.inlong.manager.pojo.consumption.ConsumptionListVo;
import org.apache.inlong.manager.pojo.consumption.ConsumptionQuery;
import org.apache.inlong.manager.pojo.consumption.ConsumptionSummary;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;

/**
 * Data consumption interface
 */
public interface ConsumptionService {

    /**
     * Data consumption statistics
     *
     * @param query Query conditions
     * @return Statistics
     */
    ConsumptionSummary getSummary(ConsumptionQuery query);

    /**
     * Get data consumption list according to query conditions
     *
     * @param query Consumption info
     * @return Consumption list
     */
    PageResult<ConsumptionListVo> list(ConsumptionQuery query);

    /**
     * Get data consumption details
     *
     * @param id Consumer ID
     * @return Details
     */
    ConsumptionInfo get(Integer id);

    /**
     * Determine whether the Consumer group already exists
     *
     * @param consumerGroup Consumer group
     * @param excludeSelfId Exclude the ID of this record
     * @return does it exist
     */
    boolean isConsumerGroupExists(String consumerGroup, Integer excludeSelfId);

    /**
     * Save basic data consumption information
     *
     * @param consumptionInfo Data consumption information
     * @param operator Operator
     * @return ID after saved
     */
    Integer save(ConsumptionInfo consumptionInfo, String operator);

    /**
     * Update the person in charge of data consumption, etc
     *
     * @param consumptionInfo consumption information
     * @param operator Operator
     */
    Boolean update(ConsumptionInfo consumptionInfo, String operator);

    /**
     * Delete data consumption
     *
     * @param id Consumer ID
     * @param operator Operator
     */
    Boolean delete(Integer id, String operator);

    /**
     * Save the consumer group info for Sort to the database
     */
    void saveSortConsumption(InlongGroupInfo bizInfo, String topic, String consumerGroup);

}
