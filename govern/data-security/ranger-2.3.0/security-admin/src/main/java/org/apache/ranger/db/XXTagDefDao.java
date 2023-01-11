/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXTagDef;
import org.springframework.stereotype.Service;

@Service
public class XXTagDefDao extends BaseDao<XXTagDef> {

	public XXTagDefDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}

	public XXTagDef findByGuid(String guid) {
		if (StringUtils.isEmpty(guid)) {
			return null;
		}

		try {
			return getEntityManager().createNamedQuery("XXTagDef.findByGuid", tClass)
					.setParameter("guid", guid).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public XXTagDef findByName(String name) {
		if (StringUtils.isEmpty(name)) {
			return null;
		}

		try {
			return getEntityManager().createNamedQuery("XXTagDef.findByName", tClass)
					.setParameter("name", name).getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

    public List<XXTagDef> findByServiceId(Long serviceId) {
        List<XXTagDef> ret = new ArrayList<>();
        if (serviceId != null) {
            List<Object[]> rows = null;
            try {
                rows = getEntityManager().createNamedQuery("XXTagDef.findByServiceId", Object[].class)
                        .setParameter("serviceId", serviceId).getResultList();
            } catch (NoResultException e) {
                // Nothing
            }
            if (CollectionUtils.isNotEmpty(rows)) {
                for (Object[] row : rows) {
                    XXTagDef xxTagDef = new XXTagDef();
                    xxTagDef.setId((Long) row[0]);
                    xxTagDef.setGuid((String) row[1]);
                    xxTagDef.setVersion((Long) row[2]);
                    xxTagDef.setIsEnabled((Boolean) row[3]);
                    xxTagDef.setName((String) row[4]);
                    xxTagDef.setSource((String) row[5]);
                    xxTagDef.setTagAttrDefs((String) row[6]);

                    ret.add(xxTagDef);
                }
            }
        }
        return ret;
    }

	public List<String> getAllNames() {
		try {
			return getEntityManager().createNamedQuery("XXTagDef.getAllNames", String.class).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<String>();
		}
	}

	public List<XXTagDef> findByResourceId(Long resourceId) {
		if (resourceId == null) {
			return new ArrayList<XXTagDef>();
		}

		try {
			return getEntityManager().createNamedQuery("XXTagDef.findByResourceId", tClass)
					.setParameter("resourceId", resourceId).getResultList();
		} catch (NoResultException e) {
			return new ArrayList<XXTagDef>();
		}
	}
}
