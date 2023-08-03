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

package org.apache.ranger.db;

import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXPolicyLabelMap;
import org.springframework.stereotype.Service;

@Service
public class XXPolicyLabelMapDao extends BaseDao<XXPolicyLabelMap> {

        public XXPolicyLabelMapDao(RangerDaoManagerBase daoMgr) {
                super(daoMgr);
        }

        public List<XXPolicyLabelMap> findByPolicyId(Long policyId) {
                if(policyId == null) {
                        return null;
                }
                try {
                        return getEntityManager()
                                        .createNamedQuery("XXPolicyLabelMap.findByPolicyId", tClass)
                                        .setParameter("policyId", policyId).getResultList();
                } catch (NoResultException e) {
                        return null;
                }
        }

        public XXPolicyLabelMap findByPolicyLabelId(Long policyLabelId) {
                if (policyLabelId == null) {
                        return null;
                }
                try {
                        return (XXPolicyLabelMap) getEntityManager().createNamedQuery("XXPolicyLabelMap.findByPolicyLabelId", tClass)
                                        .setParameter("policyLabelId", policyLabelId).getResultList();
                } catch (NoResultException e) {
                        return null;
                }
        }

        public List<XXPolicyLabelMap> findByServiceId(Long serviceId) {
                if (serviceId == null) {
                        return null;
                }
                try {
                        return getEntityManager().createNamedQuery("XXPolicyLabelMap.findByServiceId", tClass)
                                        .setParameter("serviceId", serviceId).getResultList();
                } catch (NoResultException e) {
                        return null;
                }
        }

}
