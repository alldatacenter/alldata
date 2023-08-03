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

package org.apache.ranger.unixusersync.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashMap;
import java.util.Date;
import java.util.HashSet;
import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.ranger.ugsyncutil.util.UgsyncCommonConstants;
import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.apache.ranger.ugsyncutil.model.FileSyncSourceInfo;
import org.apache.ranger.ugsyncutil.model.UgsyncAuditInfo;
import org.apache.ranger.usergroupsync.AbstractUserGroupSource;
import org.apache.ranger.usergroupsync.UserGroupSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.apache.ranger.usergroupsync.UserGroupSource;

public class FileSourceUserGroupBuilder extends AbstractUserGroupSource  implements UserGroupSource {
	private static final Logger LOG = LoggerFactory.getLogger(FileSourceUserGroupBuilder.class);

	private Map<String,List<String>> user2GroupListMap     = new HashMap<String,List<String>>();
	private String                   userGroupFilename     = null;
	private Map<String, Map<String, String>> sourceUsers; // Stores username and attr name & value pairs
	private Map<String, Map<String, String>> sourceGroups; // Stores groupname and attr name & value pairs
	private Map<String, Set<String>> sourceGroupUsers;
	private long                     usergroupFileModified = 0;
	private UgsyncAuditInfo ugsyncAuditInfo;
	private FileSyncSourceInfo				 fileSyncSourceInfo;
	private boolean isStartupFlag = false;

	private boolean isUpdateSinkSucc = true;
	private int deleteCycles;
	private boolean computeDeletes = false;
	private String currentSyncSource;

	public static void main(String[] args) throws Throwable {
		FileSourceUserGroupBuilder filesourceUGBuilder = new FileSourceUserGroupBuilder();

		if (args.length > 0) {
			filesourceUGBuilder.setUserGroupFilename(args[0]);
		}
		
		filesourceUGBuilder.init();

		UserGroupSink ugSink = UserGroupSyncConfig.getInstance().getUserGroupSink();
		LOG.info("initializing sink: " + ugSink.getClass().getName());
		ugSink.init();

		filesourceUGBuilder.updateSink(ugSink);
		
		if ( LOG.isDebugEnabled()) {
			filesourceUGBuilder.print();
		}
	}

	public FileSourceUserGroupBuilder() {
		super();
	}
	
	@Override
	public void init() throws Throwable {
		isStartupFlag = true;
		deleteCycles = 1;
		currentSyncSource = config.getCurrentSyncSource();
		if(userGroupFilename == null) {
			userGroupFilename = config.getUserSyncFileSource();
		}
		ugsyncAuditInfo = new UgsyncAuditInfo();
		fileSyncSourceInfo = new FileSyncSourceInfo();
		ugsyncAuditInfo.setSyncSource(currentSyncSource);
		ugsyncAuditInfo.setFileSyncSourceInfo(fileSyncSourceInfo);
		fileSyncSourceInfo.setFileName(userGroupFilename);
		buildUserGroupInfo();
	}
	
	@Override
	public boolean isChanged() {
		computeDeletes = false;
		// If previous update to Ranger admin fails, 
		// we want to retry the sync process even if there are no changes to the sync files
		if (!isUpdateSinkSucc) {
			LOG.info("Previous updateSink failed and hence retry!!");
			isUpdateSinkSucc = true;
			return true;
		}
		try {
			if (config.isUserSyncDeletesEnabled() && deleteCycles >= config.getUserSyncDeletesFrequency()) {
				deleteCycles = 1;
				computeDeletes = true;
				if (LOG.isDebugEnabled()) {
					LOG.debug("Compute deleted users/groups is enabled for this sync cycle");
				}
				return true;
			}
		} catch (Throwable t) {
			LOG.error("Failed to get information about usersync delete frequency", t);
		}
		if (config.isUserSyncDeletesEnabled()) {
			deleteCycles++;
		}

		long TempUserGroupFileModifedAt = new File(userGroupFilename).lastModified();
		if (usergroupFileModified != TempUserGroupFileModifedAt) {
			return true;
		}
		return false;
	}

	@Override
	public void updateSink(UserGroupSink sink) throws Throwable {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date lastModifiedTime = new Date(usergroupFileModified);
		Date syncTime = new Date(System.currentTimeMillis());
		fileSyncSourceInfo.setLastModified(formatter.format(lastModifiedTime));
		fileSyncSourceInfo.setSyncTime(formatter.format(syncTime));

		if (isChanged() || isStartupFlag) {
			buildUserGroupInfo();

			for (Map.Entry<String, List<String>> entry : user2GroupListMap.entrySet()) {
				String userName = entry.getKey();
				Map<String, String> userAttrMap = new HashMap<>();
				userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
				userAttrMap.put(UgsyncCommonConstants.FULL_NAME, userName);
				userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
				sourceUsers.put(userName, userAttrMap);
				List<String> groups = entry.getValue();
				if (groups != null) {
					for(String groupName : groups) {
						Map<String, String> groupAttrMap = new HashMap<>();
						groupAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, groupName);
						groupAttrMap.put(UgsyncCommonConstants.FULL_NAME, groupName);
						groupAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
						sourceGroups.put(groupName, groupAttrMap);
						Set<String> groupUsers = sourceGroupUsers.get(groupName);
						if (CollectionUtils.isNotEmpty(groupUsers)) {
							groupUsers.add(userName);
						} else {
							groupUsers = new HashSet<>();
							groupUsers.add(userName);
						}
						sourceGroupUsers.put(groupName, groupUsers);
					}
				}

			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Users = " + sourceUsers.keySet());
				LOG.debug("Groups = " + sourceGroups.keySet());
				LOG.debug("GroupUsers = " + sourceGroupUsers.keySet());
			}

			try {
				sink.addOrUpdateUsersGroups(sourceGroups, sourceUsers, sourceGroupUsers, computeDeletes);
			} catch (Throwable t) {
				LOG.error("Failed to update ranger admin. Will retry in next sync cycle!!", t);
				isUpdateSinkSucc = false;
			}
		}
		try {
			sink.postUserGroupAuditInfo(ugsyncAuditInfo);
		} catch (Throwable t) {
			LOG.error("sink.postUserGroupAuditInfo failed with exception: " + t.getMessage());
		}
		isStartupFlag = false;
	}

	private void setUserGroupFilename(String filename) {
		userGroupFilename = filename;
	}

	private void print() {
		for(String user : user2GroupListMap.keySet()) {
			LOG.debug("USER:" + user);
			List<String> groups = user2GroupListMap.get(user);
			if (groups != null) {
				for(String group : groups) {
					LOG.debug("\tGROUP: " + group);
				}
			}
		}
	}

	public void buildUserGroupInfo() throws Throwable {
		sourceUsers = new HashMap<>();
		sourceGroups = new HashMap<>();
		sourceGroupUsers = new HashMap<>();
		buildUserGroupList();
		if ( LOG.isDebugEnabled()) {
			print();
		}
	}
	
	public void buildUserGroupList() throws Throwable {
		if (userGroupFilename == null){
			throw new Exception("User Group Source File is not Configured. Please maintain in unixauthservice.properties or pass it as command line argument for org.apache.ranger.unixusersync.process.FileSourceUserGroupBuilder");
		}
	
		File f = new File(userGroupFilename);
		
		if (f.exists() && f.canRead()) {
			Map<String,List<String>> tmpUser2GroupListMap = null;
			
			if ( isJsonFile(userGroupFilename) ) {
				tmpUser2GroupListMap = readJSONfile(f);
			} else {
				tmpUser2GroupListMap = readTextFile(f);
			}

			if(tmpUser2GroupListMap != null) {
				user2GroupListMap     = tmpUser2GroupListMap;
				
				usergroupFileModified = f.lastModified();
			} else {
				LOG.info("No new UserGroup to sync at this time");
			}
		} else {
			throw new Exception("User Group Source File " + userGroupFilename + "doesn't not exist or readable");
		}
	}
	
	public boolean isJsonFile(String userGroupFilename) {
		boolean ret = false;

		if ( userGroupFilename.toLowerCase().endsWith(".json")) {
			ret = true;
		}

		return ret;
	}
	
	public 	Map<String, List<String>> readJSONfile(File jsonFile) throws Exception {
		Map<String, List<String>> ret = new HashMap<String, List<String>>();

		JsonReader jsonReader = new JsonReader(new BufferedReader(new FileReader(jsonFile)));
		
		Gson gson = new GsonBuilder().create();

		ret = gson.fromJson(jsonReader, ret.getClass());
		
		return ret;

	}
	
	public Map<String, List<String>> readTextFile(File textFile) throws Exception {
		
		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		
		String delimiter = config.getUserSyncFileSourceDelimiter();
		
		CSVFormat csvFormat = CSVFormat.newFormat(delimiter.charAt(0));
		
		CSVParser csvParser = new CSVParser(new BufferedReader(new FileReader(textFile)), csvFormat);
		
		List<CSVRecord> csvRecordList = csvParser.getRecords();
		
		if ( csvRecordList != null) {
			for(CSVRecord csvRecord : csvRecordList) {
				List<String> groups = new ArrayList<String>();
				String user = csvRecord.get(0);
				
				user = user.replaceAll("^\"|\"$", "");
					
				int i = csvRecord.size();
				
				for (int j = 1; j < i; j ++) {
					String group = csvRecord.get(j);
					if ( group != null && !group.isEmpty()) {
						 group = group.replaceAll("^\"|\"$", "");
						 groups.add(group);
					}
				}
				ret.put(user,groups);
			 }
		}

		csvParser.close();

		return ret;
	}

}
