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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sort.BaseSortConf;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;

import java.util.List;

public interface InlongGroup {

    /**
     * Create inlong stream
     *
     * @param streamInfo inlong stream info
     * @return inlong stream builder
     */
    InlongStreamBuilder createStream(InlongStreamInfo streamInfo) throws Exception;

    /**
     * Create snapshot for Inlong group
     *
     * @return inlong group context
     * @throws Exception the exception
     */
    InlongGroupContext context() throws Exception;

    /**
     * Init inlong group.
     * This operation will init all physical resources needed to start a stream group
     * Must be operated after all inlong streams were created;
     *
     * @return inlong group info
     */
    InlongGroupContext init() throws Exception;

    /**
     * Update Inlong group on updated conf
     *
     * Update inlong group and sort conf
     *
     * @param originGroupInfo origin group info that need to update
     * @param sortConf sort config that need to update
     * @throws Exception any exception
     */
    void update(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) throws Exception;

    /**
     * Update sort conf for Inlong group
     *
     * @param sortConf sort config that need to update
     * @throws Exception any exception
     */
    void update(BaseSortConf sortConf) throws Exception;

    /**
     * ReInit inlong group after update configuration for group.
     * Must be invoked when group is rejected, failed or started
     *
     * @return inlong group info
     */
    InlongGroupContext reInitOnUpdate(InlongGroupInfo originGroupInfo, BaseSortConf sortConf) throws Exception;

    /**
     * Suspend the stream group and return group info.
     *
     * @return group info
     */
    InlongGroupContext suspend() throws Exception;

    /**
     * Suspend the stream group and return group info.
     *
     * @return group info
     */
    InlongGroupContext suspend(boolean async) throws Exception;

    /**
     * Restart the stream group and return group info.
     *
     * @return group info
     */
    InlongGroupContext restart() throws Exception;

    /**
     * Restart the stream group and return group info.
     *
     * @return group info
     */
    InlongGroupContext restart(boolean async) throws Exception;

    /**
     * delete the stream group and return group info
     *
     * @return group info
     */
    InlongGroupContext delete() throws Exception;

    /**
     * delete the stream group and return group info
     *
     * @return group info
     */
    InlongGroupContext delete(boolean async) throws Exception;

    /**
     * List all inlong streams in certain group
     *
     * @return inlong stream contained in this group
     */
    List<InlongStream> listStreams() throws Exception;
}
