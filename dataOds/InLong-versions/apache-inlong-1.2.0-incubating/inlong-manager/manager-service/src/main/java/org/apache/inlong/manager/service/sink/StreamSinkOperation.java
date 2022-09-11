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
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;

import java.util.function.Supplier;

/**
 * Interface of the sink operation
 */
public interface StreamSinkOperation {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(SinkType sinkType);

    /**
     * Save the sink info.
     *
     * @param request The request of the sink.
     * @param operator The operator name.
     * @return sink id after saving.
     */
    default Integer saveOpt(SinkRequest request, String operator) {
        return null;
    }

    /**
     * Save sink fields via the sink request.
     *
     * @param request sink request.
     */
    default void saveFieldOpt(SinkRequest request) {
    }

    /**
     * Get sink info by the given entity.
     *
     * @param entity the given entity.
     * @return Sink info.
     */
    StreamSink getByEntity(StreamSinkEntity entity);

    /**
     * Get the target from the given entity.
     *
     * @param entity Get field value from the entity.
     * @param target Encapsulate value to the target.
     * @param <T> Type of the target.
     * @return Target after encapsulating.
     */
    <T> T getFromEntity(StreamSinkEntity entity, Supplier<T> target);

    /**
     * Get sink list response from the given sink entity page.
     *
     * @param entityPage The given entity page.
     * @return Sink list response.
     */
    default PageInfo<? extends SinkListResponse> getPageInfo(Page<StreamSinkEntity> entityPage) {
        return new PageInfo<>();
    }

    /**
     * Update the sink info.
     *
     * @param request Request of update.
     * @param operator Operator's name.
     */
    void updateOpt(SinkRequest request, String operator);

    /**
     * Update the sink fields.
     * <p/>If `onlyAdd` is <code>true</code>, only adding is allowed, modification and deletion are not allowed,
     * and the order of existing fields cannot be changed
     *
     * @param onlyAdd Whether to add fields only.
     * @param request The update request.
     */
    void updateFieldOpt(Boolean onlyAdd, SinkRequest request);

}
