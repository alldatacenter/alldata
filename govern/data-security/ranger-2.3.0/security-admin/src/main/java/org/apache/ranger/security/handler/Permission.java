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

 package org.apache.ranger.security.handler;
public class Permission {

	public static final String CREATE_PERMISSION = "CREATE";
	public static final String READ_PERMISSION = "READ";
	public static final String UPDATE_PERMISSION = "UPDATE";
	public static final String DELETE_PERMISSION = "DELETE";

	public enum permissionType {
		CREATE, READ, UPDATE, DELETE
	};

	public static permissionType getPermisson(Object in) {
		String permString = in.toString();

		if (CREATE_PERMISSION.equals(permString)) {
			return permissionType.CREATE;
		}

		if (READ_PERMISSION.equals(permString)) {
			return permissionType.READ;
		}

		if (UPDATE_PERMISSION.equals(permString)) {
			return permissionType.UPDATE;
		}

		if (DELETE_PERMISSION.equals(permString)) {
			return permissionType.DELETE;
		}

		return null;
	}
}
