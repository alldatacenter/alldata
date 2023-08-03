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

package org.apache.ranger.authorization.hive.authorizer;


import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;

import java.io.File;
import java.util.ArrayList;


public class RangerHiveResource extends RangerAccessResourceImpl {
	public static final String KEY_DATABASE = "database";
	public static final String KEY_TABLE    = "table";
	public static final String KEY_UDF      = "udf";
	public static final String KEY_COLUMN   = "column";
	public static final String KEY_URL		= "url";
	public static final String KEY_HIVESERVICE = "hiveservice";
	public static final String KEY_GLOBAL	 = "global";


	private HiveObjectType objectType = null;

	//FirstLevelResource => Database or URL or Hive Service or Global
	//SecondLevelResource => Table or UDF
	//ThirdLevelResource => column
	public RangerHiveResource(HiveObjectType objectType, String firstLevelResource) {
		this(objectType, firstLevelResource, null, null);
	}

	public RangerHiveResource(HiveObjectType objectType, String firstLevelResource, String secondLevelResource) {
		this(objectType, firstLevelResource, secondLevelResource, null);
	}
	
	public RangerHiveResource(HiveObjectType objectType, String firstLevelResource, String secondLevelResource, String thirdLevelResource) {
		this.objectType = objectType;

		switch(objectType) {
			case DATABASE:
				setValue(KEY_DATABASE, firstLevelResource);
			break;

			case FUNCTION:
				if (firstLevelResource == null) {
					firstLevelResource = "";
				}
				setValue(KEY_DATABASE, firstLevelResource);
				setValue(KEY_UDF, secondLevelResource);
			break;

			case COLUMN:
				setValue(KEY_DATABASE, firstLevelResource);
				setValue(KEY_TABLE, secondLevelResource);
				setValue(KEY_COLUMN, thirdLevelResource);
			break;

			case TABLE:
			case VIEW:
			case INDEX:
			case PARTITION:
				setValue(KEY_DATABASE, firstLevelResource);
				setValue(KEY_TABLE, secondLevelResource);
			break;

			case URI:
				ArrayList<String> objectList = new ArrayList<>();
				if (StringUtils.isNotEmpty(firstLevelResource)) {
					objectList.add(firstLevelResource);
					if (!firstLevelResource.substring(firstLevelResource.length()-1).equals(File.separator)) {
						firstLevelResource = firstLevelResource + File.separator;
						objectList.add(firstLevelResource);
					} else {
						firstLevelResource = firstLevelResource.substring(firstLevelResource.length()-2);
						objectList.add(firstLevelResource);
					}
				}
				setValue(KEY_URL,objectList);
			break;

			case SERVICE_NAME:
				if (firstLevelResource == null) {
					firstLevelResource = "";
				}
				setValue(KEY_HIVESERVICE,firstLevelResource);
			break;

			case GLOBAL:
				if (firstLevelResource == null) {
					firstLevelResource = KEY_GLOBAL;
					//There is no resource name associated to global operations
				}
				setValue(KEY_GLOBAL,firstLevelResource);
			break;

			case NONE:
			default:
			break;
		}
	}

	public HiveObjectType getObjectType() {
		return objectType;
	}

	public String getDatabase() {
		return (String) getValue(KEY_DATABASE);
	}

	public String getTable() {
		return (String) getValue(KEY_TABLE);
	}

	public String getUdf() {
		return (String) getValue(KEY_UDF);
	}

	public String getColumn() {
		return (String) getValue(KEY_COLUMN);
	}

	public String getUrl() {
		return (String) getValue(KEY_URL);
	}

	public String getHiveService() {
		return (String) getValue(KEY_HIVESERVICE);
	}
}
