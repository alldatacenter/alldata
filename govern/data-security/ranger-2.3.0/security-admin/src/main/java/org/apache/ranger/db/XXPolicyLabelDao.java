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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXPolicyLabel;
import org.springframework.stereotype.Service;

@Service
public class XXPolicyLabelDao extends BaseDao<XXPolicyLabel> {

        public XXPolicyLabelDao(RangerDaoManagerBase daoMgr) {
                super(daoMgr);
        }

        public List<XXPolicyLabel> getAllPolicyLabels() {
                try {
                        return getEntityManager().createNamedQuery("XXPolicyLabel.getAllPolicyLabels", tClass)
                                        .getResultList();
                } catch (NoResultException e) {
                        return new ArrayList<XXPolicyLabel>();
                }
        }

        public XXPolicyLabel findByName(String policyLabel) {
                if (policyLabel == null) {
                        return null;
                }
                try {
                        return getEntityManager().createNamedQuery("XXPolicyLabel.findByName", tClass)
                                        .setParameter("policyLabel", policyLabel).getSingleResult();
                } catch (NoResultException e) {
                        return null;
                }
        }

        public List<XXPolicyLabel> getByName(String policyLabel) {
            if (policyLabel == null) {
                    return null;
            }
            try {
                    return getEntityManager().createNamedQuery("XXPolicyLabel.findByName", tClass)
                                    .setParameter("policyLabel", policyLabel).getResultList();
            } catch (NoResultException e) {
                    return null;
            }
    }


        public XXPolicyLabel findByPolicyLabelId(Long policyLabelId) {
                if (policyLabelId == null) {
                        return null;
                }
                try {
                        return (XXPolicyLabel) getEntityManager().createNamedQuery("XXPolicyLabel.findByPolicyLabelId", tClass)
                                        .setParameter("policyLabelId", policyLabelId).getSingleResult();
                } catch (NoResultException e) {
                        return null;
                }
        }

                public List<XXPolicyLabel> findByServiceId(Long serviceId) {
                        if (serviceId == null) {
                                return null;
                        }
                        try {
                                return getEntityManager().createNamedQuery("XXPolicyLabel.findByServiceId", tClass)
                                                .setParameter("serviceId", serviceId).getResultList();
                        } catch (NoResultException e) {
                                return null;
                        }
                }

        public List<XXPolicyLabel> findByPolicyId(Long policyId) {
                if (policyId == null) {
                        return null;
                }
                try {
                        return getEntityManager().createNamedQuery("XXPolicyLabel.findByPolicyId", tClass)
                                        .setParameter("policyId", policyId).getResultList();
                } catch (NoResultException e) {
                        return null;
                }
        }

}
