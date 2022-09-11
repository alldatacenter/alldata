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

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEntityMapper {

    /**
     * sumByLogTs
     *
     * @param groupId The groupId of inlong
     * @param streamId The streamId of inlong
     * @param auditId The auditId of inlong
     * @param sDate The start date
     * @param eDate The end date
     * @param format The format such as '%Y-%m-%d %H:%i:00'
     * @return The result of query
     */
    List<Map<String, Object>> sumByLogTs(@Param(value = "groupId") String groupId,
            @Param(value = "streamId") String streamId,
            @Param(value = "auditId") String auditId,
            @Param(value = "sDate") String sDate,
            @Param(value = "eDate") String eDate,
            @Param(value = "format") String format);
}