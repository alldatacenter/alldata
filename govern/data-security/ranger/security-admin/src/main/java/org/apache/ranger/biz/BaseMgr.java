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

 /**
 *
 */
package org.apache.ranger.biz;

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXDBBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public abstract class BaseMgr {
    static final Logger logger = LoggerFactory.getLogger(BaseMgr.class);

    @Autowired
    RangerDaoManager daoManager;

    @Autowired
    RESTErrorUtil restErrorUtil;

    public RangerDaoManager getDaoManager() {
	return daoManager;
    }

    public void deleteEntity(BaseDao<? extends XXDBBase> baseDao, Long id,
	    String entityName) {
	XXDBBase entity = baseDao.getById(id);
	if (entity != null) {
	    try {
		baseDao.remove(id);
	    } catch (Exception e) {
		logger.error("Error deleting " + entityName + ". Id=" + id, e);
		throw restErrorUtil.createRESTException("This " + entityName
			+ " can't be deleted",
			MessageEnums.OPER_NOT_ALLOWED_FOR_STATE, id, null, ""
				+ id + ", error=" + e.getMessage());
	    }
	} else {
	    // Return without error
	    logger.info("Delete ignored for non-existent " + entityName
		    + " id=" + id);
	}
    }

    /**
     * @param objectClassType
     */
    protected void validateClassType(int objectClassType) {
	// objectClassType
	restErrorUtil.validateMinMax(objectClassType, 1,
		RangerConstants.ClassTypes_MAX, "Invalid classType", null,
		"objectClassType");
    }

}
