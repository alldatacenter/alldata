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

package org.apache.griffin.core.measure.repo;


import org.apache.griffin.core.job.repo.BaseJpaRepository;
import org.apache.griffin.core.measure.entity.Measure;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Interface to access measure repository
 *
 * @param <T> Measure and its subclass
 */
public interface MeasureRepo<T extends Measure> extends BaseJpaRepository<T, Long> {

    /**
     * search repository by name and deletion state
     *
     * @param name    query condition
     * @param deleted query condition
     * @return measure collection
     */
    List<T> findByNameAndDeleted(String name, Boolean deleted);

    /**
     * search repository by deletion state
     *
     * @param deleted query condition
     * @return measure collection
     */
    List<T> findByDeleted(Boolean deleted);

    /**
     * search repository by owner and deletion state
     *
     * @param owner   query condition
     * @param deleted query condition
     * @return measure collection
     */
    List<T> findByOwnerAndDeleted(String owner, Boolean deleted);

    /**
     * search repository by id and deletion state
     *
     * @param id      query condition
     * @param deleted query condition
     * @return measure collection
     */
    T findByIdAndDeleted(Long id, Boolean deleted);

    /**
     * search repository by deletion state
     *
     * @param deleted query condition
     * @return organization collection
     */
    @Query("select DISTINCT m.organization from #{#entityName} m "
            + "where m.deleted = ?1 and m.organization is not null")
    List<String> findOrganizations(Boolean deleted);

    /**
     * search repository by organization and deletion state
     *
     * @param organization query condition
     * @param deleted      query condition
     * @return organization collection
     */
    @Query("select m.name from #{#entityName} m "
            + "where m.organization= ?1 and m.deleted= ?2")
    List<String> findNameByOrganization(String organization, Boolean deleted);
}
