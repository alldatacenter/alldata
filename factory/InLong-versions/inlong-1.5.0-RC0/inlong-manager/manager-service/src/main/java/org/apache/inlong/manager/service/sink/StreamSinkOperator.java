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

package org.apache.inlong.manager.service.sink;

import com.github.pagehelper.Page;
import org.apache.inlong.manager.common.enums.SinkStatus;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Interface of the sink operator
 */
public interface StreamSinkOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String sinkType);

    /**
     * Save the sink info.
     *
     * @param request sink request needs to save
     * @param operator name of the operator
     * @return sink id after saving
     */
    Integer saveOpt(SinkRequest request, String operator);

    /**
     * Get the target from the given entity.
     *
     * @param entity get field value from the entity
     * @return sink info
     */
    StreamSink getFromEntity(StreamSinkEntity entity);

    /**
     * Get stream sink field list by the given sink id.
     *
     * @param sinkId sink id
     * @return stream sink field list
     */
    List<SinkField> getSinkFields(@NotNull Integer sinkId);

    /**
     * Get sink info list from the given sink entity page.
     *
     * @param entityPage sink entity page
     * @return sink info list
     */
    PageResult<? extends StreamSink> getPageInfo(Page<StreamSinkEntity> entityPage);

    /**
     * Update the sink info.
     *
     * @param request sink info needs to update
     * @param nextStatus next status
     * @param operator name of the operator
     */
    void updateOpt(SinkRequest request, SinkStatus nextStatus, String operator);

    /**
     * Update the sink fields.
     * <p/>
     * If `onlyAdd` is <code>true</code>, only adding is allowed, modification and deletion are not allowed,
     * and the order of existing fields cannot be changed
     *
     * @param onlyAdd whether to add fields only.
     * @param request sink request info needs to update
     */
    void updateFieldOpt(Boolean onlyAdd, SinkRequest request);

    /**
     * Save the sink fields.
     *
     * @param request sink request info needs to save
     */
    void saveFieldOpt(SinkRequest request);

    /**
     * Delete the sink info.
     *
     * @param entity sink info needs to delete
     * @param operator name of the operator
     */
    void deleteOpt(StreamSinkEntity entity, String operator);

    /**
     * Parse stream sink to id params
     *
     * @param streamSink
     * @return
     */
    Map<String, String> parse2IdParams(StreamSinkEntity streamSink, List<String> fields);
}
