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

import java.util.Set;

import org.apache.hadoop.hbase.security.User;

public interface HbaseUserUtils {
	/**
	 * Returns user's short name or empty string if null is passed in.
	 * @param user
	 * @return
	 */
	String getUserAsString(User user);

	/**
	 * Returns the groups to which user belongs to as known to User object.  For null values it returns an empty set.
	 * @param user
	 * @return
	 */
	Set<String> getUserGroups(User user);

	/**
	 * May return null in case of an error
	 * @return
	 */
	User getUser();
	
	/**
	 * Returns the user short name.  Returns an empty string if Hbase User of context can't be found.
	 * @param request
	 * @return
	 */
	String getUserAsString();

	/**
	 * Returns true of specified user is configured to be a super user
	 * @param user
	 * @return
	 */
	boolean isSuperUser(User user);
}
