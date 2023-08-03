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

 package org.apache.ranger.unixusersync.poc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class RangerClientUserGroupMapping {

	private static String strLine;
	private static final String TAG_USER_NAME = "name";
	private static final String TAG_USER_ID = "userId";
	private static final String TAG_GROUP_ID = "id";

	public static ArrayList<HashMap<String, String>> buildClientUserGroupMapping(String passwdFile){

	ArrayList<HashMap<String, String>> ClientUserGroupMapping = new ArrayList<HashMap<String, String>>();

	try{
		FileReader file = new FileReader(passwdFile);

	    BufferedReader br = new BufferedReader(file);


	    while ((strLine = br.readLine()) != null)  {

		ListRangerUser userList = ListRangerUser.parseUser(strLine);

            if (userList == null) {
                continue;
            }

		HashMap<String, String> map = new HashMap<String, String>();

		// adding each child node to HashMap key => value
		map.put(TAG_USER_NAME, userList.getName());
		map.put(TAG_USER_ID, userList.getUid());
		map.put(TAG_GROUP_ID, userList.getGid());

		// adding HashList to ArrayList
            ClientUserGroupMapping.add(map);

			// System.out.println(userList.getName() + " " + userList.getUid() + " " + userList.getGid());
		  }

	    file.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		return ClientUserGroupMapping;
	}
}
