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

package org.apache.inlong.manager.service.transform;

import org.apache.inlong.manager.pojo.transform.DeleteTransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

/**
 * Service layer interface for stream transform
 */
public interface StreamTransformService {

    /**
     * Save the transform information.
     *
     * @param request the transform request
     * @param operator name of the operator
     * @return transform id after saving
     */
    Integer save(TransformRequest request, String operator);

    /**
     * Save the transform information.
     *
     * @param request the transform request
     * @param opInfo userinfo of operator
     * @return transform id after saving
     */
    Integer save(TransformRequest request, UserInfo opInfo);

    /**
     * Query transform information based on inlong group id and inlong stream id.
     *
     * @param groupId the inlong group id
     * @param streamId the inlong stream id
     * @return the transform response
     */
    List<TransformResponse> listTransform(String groupId, String streamId);

    /**
     * Query transform information based on inlong group id and inlong stream id.
     *
     * @param groupId the inlong group id
     * @param streamId the inlong stream id
     * @param opInfo userinfo of operator
     * @return the transform response
     */
    List<TransformResponse> listTransform(String groupId, String streamId, UserInfo opInfo);

    /**
     * Modify data transform information.
     *
     * @param request the transform request
     * @param operator name of the operator
     * @return Whether succeed
     */
    Boolean update(TransformRequest request, String operator);

    /**
     * Modify data transform information.
     *
     * @param request the transform request
     * @param opInfo userinfo of operator
     * @return Whether succeed
     */
    Boolean update(TransformRequest request, UserInfo opInfo);

    /**
     * Delete the stream transform by the given id.
     *
     * @param request delete request
     * @param operator name of the operator
     * @return Whether succeed
     */
    Boolean delete(DeleteTransformRequest request, String operator);

    /**
     * Delete the stream transform by the given id.
     *
     * @param request delete request
     * @param opInfo userinfo of operator
     * @return Whether succeed
     */
    Boolean delete(DeleteTransformRequest request, UserInfo opInfo);

}
