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
package org.apache.ranger.authorization.hbase;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HbaseUserUtilsImpl implements HbaseUserUtils {

	private static final Logger LOG = LoggerFactory.getLogger(HbaseUserUtilsImpl.class.getName());
	private static final String SUPERUSER_CONFIG_PROP = "hbase.superuser";

	// only to detect problems with initialization order, not for thread-safety.
	static final AtomicBoolean _Initialized = new AtomicBoolean(false);
	// should never be null
	static final AtomicReference<Set<String>> _SuperUsers = new AtomicReference<Set<String>>(new HashSet<String>());
	
	public static void initiailize(Configuration conf) {
		
		if (_Initialized.get()) {
			LOG.warn("HbaseUserUtilsImpl.initialize: Unexpected: initialization called more than once!");
		} else {
			if (conf == null) {
				LOG.error("HbaseUserUtilsImpl.initialize: Internal error: called with null conf value!");
			} else {
				String[] users = conf.getStrings(SUPERUSER_CONFIG_PROP);
				if (users != null && users.length > 0) {
					Set<String> superUsers = new HashSet<String>(users.length);
					for (String user : users) {
						user = user.trim();
						LOG.info("HbaseUserUtilsImpl.initialize: Adding Super User(" + user + ")");
						superUsers.add(user);
					}
					_SuperUsers.set(superUsers);
				}
			}
			_Initialized.set(true);
		}
	}
	
	@Override
	public String getUserAsString(User user) {
		if (user == null) {
			throw new IllegalArgumentException("User is null!");
		}
		else {
			return user.getShortName();
		}
	}

	@Override
	public Set<String> getUserGroups(User user) {
		if (user == null) {
			throw new IllegalArgumentException("User is null!");
		}
		else {
			String[] groupsArray = user.getGroupNames();
			return new HashSet<String>(Arrays.asList(groupsArray));
		}
	}

	@Override
	public User getUser() {
		// current implementation does not use the request object!
		User user = null;
		try {
			user = RpcServer.getRequestUser().get();
		} catch (NoSuchElementException e) {
			LOG.info("Unable to get request user");
		}
		if (user == null) {
			try {
				user = User.getCurrent();
			} catch (IOException e) {
				LOG.error("Unable to get current user: User.getCurrent() threw IOException");
				user = null;
			}
		}
		return user;
	}


	@Override
	public String getUserAsString() {
		User user = getUser();
		if (user == null) {
			return "";
		}
		else {
			return getUserAsString(user);
		}
	}

	/**
	 * No user can be a superuser till the class is properly initialized.  Once class is properly initialized, users specified in
	 * configuration would be reported as super users.
	 */
	@Override
	public boolean isSuperUser(User user) {
		if (!_Initialized.get()) {
			LOG.error("HbaseUserUtilsImpl.isSuperUser: Internal error: called before initialization was complete!");
		}
		Set<String> superUsers = _SuperUsers.get(); // can never be null
		boolean isSuper = superUsers.contains(user.getShortName());
		if (LOG.isDebugEnabled()) {
			LOG.debug("IsSuperCheck on [" + user.getShortName() + "] returns [" + isSuper + "]");
		}
		return isSuper;
	}
}
