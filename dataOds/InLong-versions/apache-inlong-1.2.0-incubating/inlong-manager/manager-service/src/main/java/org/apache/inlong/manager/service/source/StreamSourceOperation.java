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

package org.apache.inlong.manager.service.source;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.pojo.source.SourceRequest;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;

import java.util.function.Supplier;

/**
 * Interface of the source operation
 */
public interface StreamSourceOperation {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(SourceType sourceType);

    /**
     * Save the source info.
     *
     * @param request request of source
     * @param groupStatus the belongs group status
     * @param operator name of operator
     * @return source id after saving
     */
    Integer saveOpt(SourceRequest request, Integer groupStatus, String operator);

    /**
     * Get source info by the given entity.
     *
     * @param entity get field value from the entity
     * @return source info
     */
    StreamSource getByEntity(StreamSourceEntity entity);

    /**
     * Get the target from the given entity.
     *
     * @param entity get field value from the entity
     * @param target encapsulate value to the target
     * @param <T> target type
     * @return target after encapsulating.
     */
    <T> T getFromEntity(StreamSourceEntity entity, Supplier<T> target);

    /**
     * Get source list response from the given source entity page.
     *
     * @param entityPage given entity page
     * @return source list response
     */
    PageInfo<? extends SourceListResponse> getPageInfo(Page<StreamSourceEntity> entityPage);

    /**
     * Update the source info.
     *
     * @param request request of source
     * @param groupStatus the belongs group status
     * @param operator name of operator
     */
    void updateOpt(SourceRequest request, Integer groupStatus, String operator);

    /**
     * Stop the source task.
     *
     * @param request request of source
     * @param operator name of operator
     */
    void stopOpt(SourceRequest request, String operator);

    /**
     * Restart the source task.
     *
     * @param request request of source
     * @param operator name of operator
     */
    void restartOpt(SourceRequest request, String operator);

}
