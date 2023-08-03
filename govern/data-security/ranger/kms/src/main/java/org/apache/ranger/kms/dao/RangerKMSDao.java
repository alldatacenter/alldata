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

import java.util.ArrayList;
import java.util.List;

import org.apache.ranger.entity.XXRangerKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

public class RangerKMSDao extends BaseDao<XXRangerKeyStore> {
    private static final Logger logger = LoggerFactory.getLogger(RangerKMSDao.class);

    private static final String GET_ALL_KEYS_QUERY_NAME = "XXRangerKeyStore.getAllKeys";

    RangerKMSDao(DaoManagerBase daoManager) {
        super(daoManager);
    }

    public XXRangerKeyStore findByAlias(String alias){
        return super.findByAlias("XXRangerKeyStore.findByAlias", alias);
    }

    public int deleteByAlias(String alias){
        return super.deleteByAlias("XXRangerKeyStore.deleteByAlias", alias);
    }

    public List<XXRangerKeyStore> getAllKeys() {
        List<XXRangerKeyStore> ret = null;
        EntityManager          em  = null;

        try {
            em = getEntityManager();

            List<Object[]> rows = (List<Object[]>) em.createNamedQuery(GET_ALL_KEYS_QUERY_NAME)
                                                     .setHint("eclipselink.refresh", "true")
                                                     .getResultList();

            if (rows != null) {
                ret = new ArrayList<>(rows.size());

                for (Object[] row : rows) {
                    XXRangerKeyStore key = new XXRangerKeyStore();

                    key.setId((Long) row[0]);
                    key.setAlias((String) row[1]);
                    key.setCreatedDate((Long) row[2]);
                    key.setEncoded((String) row[3]);
                    key.setCipher((String) row[4]);
                    key.setBitLength((Integer) row[5]);
                    key.setDescription((String) row[6]);
                    key.setVersion((Integer) row[7]);
                    key.setAttributes((String) row[8]);

                    ret.add(key);
                }
            }
        } catch (NoResultException e) {
            logger.error("getAllKeys({}) failed", GET_ALL_KEYS_QUERY_NAME, e);
        } finally {
            if (em != null) {
                em.clear();
            }
        }

        return ret;
    }
}
