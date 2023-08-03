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

package org.apache.ranger.services.elasticsearch.privilege;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class IndexPrivilegeUtils {
	// These privileges have priority, should be matched in order
	private static final List<IndexPrivilege> privileges = new LinkedList<>();

	// External Privileges for users
	public static final String ALL = "all";
	public static final String MONITOR = "monitor";
	public static final String MANAGE = "manage";
	public static final String VIEW_INDEX_METADATA = "view_index_metadata";
	public static final String READ = "read";
	public static final String READ_CROSS_CLUSTER = "read_cross_cluster";
	public static final String INDEX = "index";
	public static final String CREATE = "create";
	public static final String DELETE = "delete";
	public static final String WRITE = "write";
	public static final String DELETE_INDEX = "delete_index";
	public static final String CREATE_INDEX = "create_index";

	// Internal Privileges for Ranger authentication
	public static final String INDICES_PUT = "indices_put";
	public static final String INDICES_SEARCH_SHARDS = "indices_search_shards";
	public static final String INDICES_BULK = "indices_bulk";
	public static final String INDICES_INDEX = "indices_index";

	static {
		// First Priority
		privileges.add(new IndexPrivilege(VIEW_INDEX_METADATA,
				Arrays.asList("indices:admin/aliases/get", "indices:admin/aliases/exists", "indices:admin/get",
						"indices:admin/exists", "indices:admin/mappings/fields/get", "indices:admin/mappings/get",
						"indices:admin/types/exists", "indices:admin/validate/query", "indices:monitor/settings/get")));
		privileges.add(new IndexPrivilege(READ, Arrays.asList("indices:data/read/")));
		privileges.add(
				new IndexPrivilege(READ_CROSS_CLUSTER, Arrays.asList("internal:transport/proxy/indices:data/read/")));
		privileges.add(new IndexPrivilege(INDEX, Arrays.asList("indices:data/write/update")));

		privileges.add(new IndexPrivilege(DELETE, Arrays.asList("indices:data/write/delete")));
		privileges.add(new IndexPrivilege(DELETE_INDEX, Arrays.asList("indices:admin/delete")));
		privileges.add(new IndexPrivilege(CREATE_INDEX, Arrays.asList("indices:admin/create")));

		privileges.add(new IndexPrivilege(INDICES_PUT, Arrays.asList("indices:admin/mapping/put")));
		privileges.add(new IndexPrivilege(INDICES_SEARCH_SHARDS, Arrays.asList("indices:admin/shards/search_shards")));
		privileges.add(new IndexPrivilege(INDICES_BULK, Arrays.asList("indices:data/write/bulk")));
		privileges.add(new IndexPrivilege(INDICES_INDEX, Arrays.asList("indices:data/write/index")));

		// Second Priority
		privileges.add(new IndexPrivilege(MONITOR, Arrays.asList("indices:monitor/")));
		privileges.add(new IndexPrivilege(MANAGE, Arrays.asList("indices:admin/")));
		privileges.add(new IndexPrivilege(WRITE, Arrays.asList("indices:data/write/")));

		// Last Priority
		privileges.add(
				new IndexPrivilege(ALL, Arrays.asList("indices:", "internal:transport/proxy/indices:", "cluster:")));

	}

	/**
	 * If action is empty or not matched, set default privilege "all".
	 * @param action
	 * @return privilege
	 */
	public static String getPrivilegeFromAction(String action) {
		if (StringUtils.isEmpty(action)) {
			return ALL;
		}

		for (IndexPrivilege privilege : privileges) {
			// Get the privilege of matched action rule in order
			for (String actionRule : privilege.getActions()) {
				if (action.startsWith(actionRule)) {
					return privilege.getPrivilege();
				}
			}
		}

		return ALL;
	}
}
