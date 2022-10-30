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

import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;

public interface ClusterConfigMapper extends AbstractMapper {

    /**
     * Add or replace cluster setting info into store
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result   process result with old value
     * @return  the process result
     */
    boolean addUpdClusterConfig(ClusterSettingEntity entity,
                                StringBuilder strBuff, ProcessResult result);

    /**
     * delete current cluster setting from store
     *
     * @param result the process result
     * @param strBuff  the string buffer
     * @return  the process result
     */
    boolean delClusterConfig(StringBuilder strBuff, ProcessResult result);

    /**
     * get current cluster setting from store
     *
     * @return current cluster setting, null or object, only read
     */
    ClusterSettingEntity getClusterConfig();
}
