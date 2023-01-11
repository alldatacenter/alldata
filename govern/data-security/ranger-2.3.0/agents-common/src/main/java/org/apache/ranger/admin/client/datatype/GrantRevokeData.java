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

 package org.apache.ranger.admin.client.datatype;


import java.util.List;
import java.util.ArrayList;

import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.plugin.util.JsonUtilsV2;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrantRevokeData implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	private String        grantor;
	private String        repositoryName;
	private String        repositoryType;
	private String        databases;
	private String        tables;
	private String        columns;
	private String        columnFamilies;
	private List<PermMap> permMapList = new ArrayList<>();
	
	private static String WILDCARD_ASTERISK = "*";

	public GrantRevokeData() {
	}

	public String getGrantor() {
		return grantor;
	}

	public void setGrantor(String grantor) {
		this.grantor = grantor;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public String getRepositoryType() {
		return repositoryType;
	}

	public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
	}

	public String getDatabases() {
		return databases;
	}

	public void setDatabases(String databases) {
		this.databases = databases;
	}

	public String getTables() {
		return tables;
	}

	public void setTables(String tables) {
		this.tables = tables;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getColumnFamilies() {
		return columnFamilies;
	}

	public void setColumnFamilies(String columnFamilies) {
		this.columnFamilies = columnFamilies;
	}

	public List<PermMap> getPermMapList() {
		return permMapList;
	}

	public void setPermMapList(List<PermMap> permMapList) {
		this.permMapList = permMapList;
	}


	public void setHiveData(String  grantor,
							String  repositoryName,
							String  databases,
							String  tables,
							String  columns,
							PermMap permMap) {
		this.grantor         = grantor;
		this.repositoryName = repositoryName;
		this.repositoryType = "hive";
		this.databases      = StringUtil.isEmpty(databases) ? WILDCARD_ASTERISK : databases;
		this.tables         = StringUtil.isEmpty(tables)    ? WILDCARD_ASTERISK : tables;
		this.columns        = StringUtil.isEmpty(columns)   ? WILDCARD_ASTERISK : columns;
		this.permMapList.add(permMap);
	}

	public void setHBaseData(String  grantor,
							 String  repositoryName,
							 String  tables,
							 String  columns,
							 String  columnFamilies,
							 PermMap permMap) {
		this.grantor         = grantor;
		this.repositoryName = repositoryName;
		this.repositoryType = "hbase";
		this.tables         = StringUtil.isEmpty(tables)         ? WILDCARD_ASTERISK : tables;
		this.columns        = StringUtil.isEmpty(columns)        ? WILDCARD_ASTERISK : columns;
		this.columnFamilies = StringUtil.isEmpty(columnFamilies) ? WILDCARD_ASTERISK : columnFamilies;
		this.permMapList.add(permMap);
	}
	
	public String toJson() {
		try {
			return JsonUtilsV2.objToJson(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "";
	}

	@Override
	public String toString() {
		return toJson();
	}

	@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class PermMap implements java.io.Serializable {
		private List<String> userList  = new ArrayList<>();
		private List<String> groupList = new ArrayList<>();
		private List<String> permList  = new ArrayList<>();

		public PermMap() {
		}

		public PermMap(String user, String group, String perm) {
			addUser(user);
			addGroup(group);
			addPerm(perm);
		}

		public PermMap(List<String> userList, List<String> groupList, List<String> permList) {
			copyList(userList, this.userList);
			copyList(groupList, this.groupList);
			copyList(permList, this.permList);
		}

		public List<String> getUserList() {
			return userList;
		}

		public List<String> getGroupList() {
			return groupList;
		}

		public List<String> getPermList() {
			return permList;
		}

		public void addUser(String user) {
			addToList(user, userList);
		}

		public void addGroup(String group) {
			addToList(group, groupList);
		}

		public void addPerm(String perm) {
			addToList(perm, permList);
		}

		private void addToList(String str, List<String> list) {
			if(list != null && !StringUtil.isEmpty(str)) {
				list.add(str);
			}
		}

		private void copyList(List<String> fromList, List<String> toList) {
			if(fromList != null && toList != null) {
				for(String str : fromList) {
					addToList(str, toList);
				}
			}
		}

		public String toJson() {
			try {
				return JsonUtilsV2.objToJson(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return "";
		}

		@Override
		public String toString() {
			return toJson();
		}
	}
	
	public static void main(String[] args) {
		GrantRevokeData grData = new GrantRevokeData();
		
		System.out.println(grData.toString());
	}
}
