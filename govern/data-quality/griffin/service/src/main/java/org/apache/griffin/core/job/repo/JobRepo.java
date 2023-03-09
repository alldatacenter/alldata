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

import org.apache.griffin.core.job.entity.AbstractJob;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepo<T extends AbstractJob> extends BaseJpaRepository<T, Long> {

    @Query("select count(j) from #{#entityName} j " +
        "where j.jobName = ?1 and j.deleted = ?2")
    int countByJobNameAndDeleted(String jobName, Boolean deleted);

    List<T> findByDeleted(boolean deleted);

    List<T> findByJobNameAndDeleted(String jobName, boolean deleted);

    List<T> findByMeasureIdAndDeleted(Long measureId, boolean deleted);

    T findByIdAndDeleted(Long jobId, boolean deleted);
}
