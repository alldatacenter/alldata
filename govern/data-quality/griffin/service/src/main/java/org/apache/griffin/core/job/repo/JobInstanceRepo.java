/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.griffin.core.job.repo;

import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.apache.griffin.core.job.entity.LivySessionStates.State;

public interface JobInstanceRepo extends BaseJpaRepository<JobInstanceBean, Long> {

    JobInstanceBean findByPredicateName(String name);

    @Query("select s from JobInstanceBean s where s.id = ?1")
    JobInstanceBean findByInstanceId(Long id);

    @Query("select s from JobInstanceBean s where s.job.id = ?1")
    List<JobInstanceBean> findByJobId(Long jobId, Pageable pageable);

    @Query("select s from JobInstanceBean s where s.job.id = ?1")
    List<JobInstanceBean> findByJobId(Long jobId);

    List<JobInstanceBean> findByExpireTmsLessThanEqual(Long expireTms);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("delete from JobInstanceBean j " +
            "where j.expireTms <= ?1 and j.deleted = false ")
    int deleteByExpireTimestamp(Long expireTms);

    @Query("select DISTINCT s from JobInstanceBean s where s.state in ?1")
    List<JobInstanceBean> findByActiveState(State[] states);

    List<JobInstanceBean> findByTriggerKey(String triggerKey);
}
