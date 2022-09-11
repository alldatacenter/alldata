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
import org.apache.inlong.manager.common.pojo.cluster.InlongClusterPageRequest;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InlongClusterEntityMapper {

    int insert(InlongClusterEntity record);

    int insertSelective(InlongClusterEntity record);

    InlongClusterEntity selectById(Integer id);

    /**
     * Select clusters by tag, name and type, the tag and name can be null.
     */
    List<InlongClusterEntity> selectByKey(@Param("clusterTag") String clusterTag, @Param("name") String name,
            @Param("type") String type);

    List<InlongClusterEntity> selectByCondition(InlongClusterPageRequest request);

    int updateByIdSelective(InlongClusterEntity record);

    int updateById(InlongClusterEntity record);

    int deleteByPrimaryKey(Integer id);

}
