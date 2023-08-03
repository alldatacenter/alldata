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

 package org.apache.ranger.common.db;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.entity.XXDBBase;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPABeanCallbacks {
	private static final Logger logger = LoggerFactory.getLogger(JPABeanCallbacks.class);

	@PrePersist
	void onPrePersist(Object o) {
		try {
			if (o != null && o instanceof XXDBBase) {
				XXDBBase entity = (XXDBBase) o;

				entity.setUpdateTime(DateUtil.getUTCDate());
				if (entity.getAddedByUserId() == null || entity.getAddedByUserId() == 0) {

					if (logger.isDebugEnabled()) {
						logger.debug("AddedByUserId is null or 0 and hence getting it from userSession for " + entity.getId());
					}
					RangerSecurityContext context = RangerContextHolder
							.getSecurityContext();
					if (context != null) {
						UserSessionBase userSession = context.getUserSession();
						if (userSession != null) {
							entity.setAddedByUserId(userSession.getUserId());
							entity.setUpdatedByUserId(userSession
									.getUserId());
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("User session not found for this request. Identity of originator of this change cannot be recorded");
							}
						}
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("Security context not found for this request. Identity of originator of this change cannot be recorded");
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error("", t);
		}

	}

	// @PostPersist
	// void onPostPersist(Object o) {
	// if (o != null && o instanceof MBase) {
	// MBase entity = (MBase) o;
	// if (logger.isDebugEnabled()) {
	// logger.debug("DBChange.create:class=" + o.getClass().getName()
	// + entity.getId());
	// }
	//
	// }
	// }

	// @PostLoad void onPostLoad(Object o) {}

	@PreUpdate
	void onPreUpdate(Object o) {
		try {
			if (o != null && o instanceof XXDBBase) {
				XXDBBase entity = (XXDBBase) o;
				entity.setUpdateTime(DateUtil.getUTCDate());
			}
		} catch (Throwable t) {
			logger.error("", t);
		}

	}

	// @PostUpdate
	// void onPostUpdate(Object o) {
	// }

	// @PreRemove void onPreRemove(Object o) {}

	// @PostRemove
	// void onPostRemove(Object o) {
	// }

}
