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


public class RangerUserGroupMapping {

	/*
private static final String TAG_XGROUP_USERS ="vXGroupUsers";
private static final String TAG_USER_NAME = "name";
private static final String TAG_USER_ID = "userId";
//private static final String TAG_GROUP_NAME = "name";
private static final String TAG_GROUP_ID = "id";

public static JSONArray vXGroupUsers = null;

public static ArrayList<HashMap<String, String>> buildUserGroupMapping(String url){

	//HashMap for UserGroupMapping

	ArrayList<HashMap<String, String>> UserGroupMapping = new ArrayList<HashMap<String, String>>();

    // Creating JSON Parser instance
    RangerJSONParser jParser = new RangerJSONParser();

    // getting JSON string from URL
    JSONObject json = jParser.getJSONFromUrl(url);

    try {
        // Getting Array of vXGroupUsers

	vXGroupUsers = json.getJSONArray(TAG_XGROUP_USERS);

        // looping through All vXGroupUsers

        for(int i = 0; i < vXGroupUsers.length(); i++) {

            JSONObject xausergroup = vXGroupUsers.getJSONObject(i);

            // Storing each json item in variable
            String uname = xausergroup.getString(TAG_USER_NAME);
            String uid = xausergroup.getString(TAG_USER_ID);
            String gid = xausergroup.getString(TAG_GROUP_ID);


            // creating new HashMap
            HashMap<String, String> map = new HashMap<String, String>();

            // adding each child node to HashMap key => value
            map.put(TAG_USER_NAME, uname);
            map.put(TAG_USER_ID, uid);
            map.put(TAG_GROUP_ID, gid);

            // adding HashList to ArrayList
            UserGroupMapping.add(map);
            }

        } catch (JSONException e) {
        e.printStackTrace();
	}

     return UserGroupMapping;

    }
    */
}
