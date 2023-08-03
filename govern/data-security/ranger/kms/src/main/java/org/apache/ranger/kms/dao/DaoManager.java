/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.kms.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

public class DaoManager extends DaoManagerBase {
    private final ThreadLocal<EntityManager> entityManagers = new ThreadLocal<>();

    @PersistenceContext
    private final EntityManagerFactory emf;

    public DaoManager(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public EntityManager getEntityManager() {
        EntityManager        ret = null;
        EntityManagerFactory emf = this.emf;

        if (emf != null) {
            ret = entityManagers.get();

            if (ret == null) {
                ret = emf.createEntityManager();

                entityManagers.set(ret);
            }
        }

        return ret;
    }
}
