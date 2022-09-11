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

package org.apache.inlong.tubemq.manager.repository;

import java.util.List;

import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterRepository extends JpaRepository<MasterEntry, Long> {

    /**
     * find master By clusterId
     *
     * @param clusterId
     * @return
     */
    MasterEntry findMasterEntryByClusterIdEquals(long clusterId);

    /**
     * find all nodes in cluster
     *
     * @param clusterId
     * @return
     */
    List<MasterEntry> findMasterEntriesByClusterIdEquals(long clusterId);

    /**
     * find all nodes
     *
     * @return
     */
    List<MasterEntry> findAll();

    /**
     *
     * find all nodes in ip
     *
     * @return
     */
    List<MasterEntry> findMasterEntryByIpEquals(String masterIp);

    /**
     * delete master by cluster id
     *
     * @return
     */
    Integer deleteByClusterId(Long clusterId);
}
