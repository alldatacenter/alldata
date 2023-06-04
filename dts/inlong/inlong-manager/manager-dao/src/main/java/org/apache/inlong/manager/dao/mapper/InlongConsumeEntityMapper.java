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
import org.apache.inlong.manager.dao.entity.InlongConsumeEntity;
import org.apache.inlong.manager.pojo.common.CountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InlongConsumeEntityMapper {

    int insert(InlongConsumeEntity record);

    InlongConsumeEntity selectById(Integer id);

    List<CountInfo> countByUser(@Param(value = "username") String username);

    InlongConsumeEntity selectExists(@Param("consumerGroup") String consumerGroup, @Param("topic") String topic,
            @Param("inlongGroupId") String inlongGroupId);

    List<InlongConsumeEntity> selectByCondition(InlongConsumePageRequest request);

    List<InlongConsumeBriefInfo> selectBriefList(InlongConsumePageRequest request);

    int updateById(InlongConsumeEntity record);

    int updateByIdSelective(InlongConsumeEntity record);

    void updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("modifier") String modifier);

    int deleteById(Integer id);

}
