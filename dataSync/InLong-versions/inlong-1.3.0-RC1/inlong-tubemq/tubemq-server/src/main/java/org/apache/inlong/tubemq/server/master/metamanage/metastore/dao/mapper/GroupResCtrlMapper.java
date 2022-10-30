/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper;

import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;

public interface GroupResCtrlMapper extends AbstractMapper {

    boolean addGroupResCtrlConf(GroupResCtrlEntity entity,
                                StringBuilder strBuff, ProcessResult result);

    boolean updGroupResCtrlConf(GroupResCtrlEntity entity,
                                StringBuilder strBuff, ProcessResult result);

    boolean delGroupResCtrlConf(String groupName, StringBuilder strBuff, ProcessResult result);

    /**
     * Get group resource control entity by group name
     *
     * @param groupName  need query group name
     * @return  group resource control info by groupName's key
     */
    GroupResCtrlEntity getGroupResCtrlConf(String groupName);

    /**
     * Get group resource control entity
     *
     * @param groupNameSet need query group name set
     * @param qryEntity   must not null
     * @return  group resource control info by groupName's key
     */
    Map<String, GroupResCtrlEntity> getGroupResCtrlConf(Set<String> groupNameSet,
                                                        GroupResCtrlEntity qryEntity);
}
