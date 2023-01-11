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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.entity.XXRMSNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 */
@Service
public class XXRMSNotificationDao extends BaseDao<XXRMSNotification> {

	private static final Logger LOG = LoggerFactory.getLogger(XXRMSNotificationDao.class);

	public XXRMSNotificationDao(RangerDaoManagerBase daoManager) {
		super(daoManager);
	}
	
	public List<XXRMSNotification> getResource() {
		List<XXRMSNotification> allResource = getAll();
		return allResource;
	}

	public Long getMaxIdOfNotifications(long llServiceId, long hlServiceId) {
		Long lastNotificationId = 0L;
		try {
			lastNotificationId = getEntityManager()
					.createNamedQuery("XXRMSNotification.getMaxIdOfNotifications", Long.class)
					.setParameter("llServiceId", llServiceId)
					.setParameter("hlServiceId", hlServiceId)
					.getSingleResult();

		} catch (NoResultException e) {
			LOG.debug(e.getMessage());
		} finally{
			if (lastNotificationId == null){
				lastNotificationId = 0L;
			}
		}
		return lastNotificationId;
	}

	public List<XXRMSNotification> getAllAfterNotificationId(long llServiceId, long hlServiceId, long notificationId) {
		List<XXRMSNotification> notifications = new ArrayList<>();
		try {
			notifications = getEntityManager()
					.createNamedQuery("XXRMSNotification.getAllAfterNotificationId", XXRMSNotification.class)
					.setParameter("llServiceId", llServiceId)
					.setParameter("hlServiceId", hlServiceId)
					.setParameter("notificationId", notificationId)
					.getResultList();
		} catch (NoResultException e) {
			LOG.debug("There are no relevant notifications after notification_id:[" + notificationId + "]");
		}
		return notifications;
	}

	public Long findLatestInvalidNotificationId(long llServiceId, long hlServiceId, long lastKnownVersion) {
		Long latestInvalidNotificationId = -1L;

		List<XXRMSNotification> notifications = getNotificationWithTypeAfterNotificationId(llServiceId, hlServiceId, "invalid", lastKnownVersion);

		if (CollectionUtils.isNotEmpty(notifications)) {
			latestInvalidNotificationId = notifications.get(notifications.size()-1).getNotificationId();
		}

		return latestInvalidNotificationId;
	}

	public List<XXRMSNotification> getNotificationWithTypeAfterNotificationId(long llServiceId, long hlServiceId, String changeType, long notificationId) {
		List<XXRMSNotification> notifications = new ArrayList<>();
		try {
			notifications = getEntityManager()
					.createNamedQuery("XXRMSNotification.getNotificationWithTypeAfterNotificationId", XXRMSNotification.class)
					.setParameter("llServiceId", llServiceId)
					.setParameter("hlServiceId", hlServiceId)
					.setParameter("changeType", changeType)
					.setParameter("notificationId", notificationId)
					.getResultList();
		} catch (NoResultException e) {

		}
		return notifications;
	}

	public List<XXRMSNotification> getDeletedNotificationsByHlResourceId(long hlResourceId, long lastKnownVersion) {
		List<XXRMSNotification> notifications = new ArrayList<>();
		try {
			notifications = getEntityManager()
					.createNamedQuery("XXRMSNotification.getDeletedNotificationsByHlResourceId", XXRMSNotification.class)
					.setParameter("hlResourceId", hlResourceId)
					.setParameter("lastKnownVersion", lastKnownVersion)
					.getResultList();
		} catch (NoResultException e) {

		}
		return notifications;
	}
}
