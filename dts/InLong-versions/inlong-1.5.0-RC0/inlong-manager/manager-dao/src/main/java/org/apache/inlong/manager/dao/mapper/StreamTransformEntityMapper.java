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

package org.apache.inlong.manager.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.inlong.manager.dao.entity.StreamTransformEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamTransformEntityMapper {

    int insert(StreamTransformEntity record);

    int insertSelective(StreamTransformEntity record);

    StreamTransformEntity selectById(Integer id);

    List<StreamTransformEntity> selectByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("transformName") String transformName);

    int updateById(StreamTransformEntity record);

    int updateByIdSelective(StreamTransformEntity record);

    int deleteById(Integer id);

    /**
     * Physically delete all stream transforms based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
