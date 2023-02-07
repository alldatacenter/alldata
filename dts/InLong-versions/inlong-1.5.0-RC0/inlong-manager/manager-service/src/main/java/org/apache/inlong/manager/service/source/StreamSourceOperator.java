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
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface of the source operator
 */
public interface StreamSourceOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String sourceType);

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
    StreamSource getFromEntity(StreamSourceEntity entity);

    /**
     * Get stream source field list by the given source id.
     *
     * @param sourceId source id
     * @return stream field list
     */
    List<StreamField> getSourceFields(@NotNull Integer sourceId);

    /**
     * Get the StreamSource Map by the inlong group info and inlong stream info list.
     *
     * @param groupInfo inlong group info
     * @param streamInfos inlong stream info list
     * @param streamSources stream source list
     * @return map of StreamSource list, key-inlongStreamId, value-StreamSourceList
     * @apiNote The MQ source which was used in InlongGroup must implement the method.
     */
    default Map<String, List<StreamSource>> getSourcesMap(InlongGroupInfo groupInfo,
            List<InlongStreamInfo> streamInfos, List<StreamSource> streamSources) {
        return new HashMap<>();
    }

    /**
     * Get source list response from the given source entity page.
     *
     * @param entityPage given entity page
     * @return source list response
     */
    PageResult<? extends StreamSource> getPageInfo(Page<StreamSourceEntity> entityPage);

    /**
     * Update the source info.
     *
     * @param request request of source
     * @param groupStatus the belongs group status
     * @param operator name of operator
     */
    void updateOpt(SourceRequest request, Integer groupStatus, Integer groupMode, String operator);

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
