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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;
import java.util.Iterator;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.ranger.ugsyncutil.model.UgsyncAuditInfo;
import org.apache.ranger.ugsyncutil.model.UnixSyncSourceInfo;
import org.apache.ranger.ugsyncutil.util.UgsyncCommonConstants;
import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.apache.ranger.usergroupsync.UserGroupSink;
import org.apache.ranger.usergroupsync.UserGroupSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnixUserGroupBuilder implements UserGroupSource {

	private static final Logger LOG = LoggerFactory.getLogger(UnixUserGroupBuilder.class);
	private final static String OS = System.getProperty("os.name");

	// kept for legacy support
	//public static final String UNIX_USER_PASSWORD_FILE = "/etc/passwd";
	//public static final String UNIX_GROUP_FILE = "/etc/group";

	/** Shell commands to get users and groups */
	static final String LINUX_GET_ALL_USERS_CMD = "getent passwd";
	static final String LINUX_GET_ALL_GROUPS_CMD = "getent group";
	static final String LINUX_GET_GROUP_CMD = "getent group %s";

	// mainly for testing purposes, there might be a better way
	static final String MAC_GET_ALL_USERS_CMD = "dscl . -readall /Users UniqueID PrimaryGroupID | " +
			"awk 'BEGIN { OFS = \":\"; ORS=\"\\n\"; i=0;}" +
			"/RecordName: / {name = $2;i = 0;}/PrimaryGroupID: / {gid = $2;}" +
			"/^ / {if (i == 0) { i++; name = $1;}}" +
			"/UniqueID: / {uid = $2;print name, \"*\", gid, uid;}'";
	static final String MAC_GET_ALL_GROUPS_CMD = "dscl . -list /Groups PrimaryGroupID | " +
			"awk -v OFS=\":\" '{print $1, \"*\", $2, \"\"}'";
	static final String MAC_GET_GROUP_CMD = "dscl . -read /Groups/%1$s | paste -d, -s - | sed -e 's/:/|/g' | " +
			"awk -v OFS=\":\" -v ORS=\"\\n\" -F, '{print \"%1$s\",\"*\",$6,$4}' | " +
			"sed -e 's/:[^:]*| /:/g' | sed -e 's/ /,/g'";

	static final String BACKEND_PASSWD = "passwd";

	private boolean isUpdateSinkSucc = true;
	private boolean enumerateGroupMembers = false;
	private boolean useNss = false;

	private long lastUpdateTime = 0; // Last time maps were updated
	private long timeout = 0;

	private UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();
	private Map<String,String> groupId2groupNameMap;
	private Map<String, Map<String, String>> sourceUsers; // Stores username and attr name & value pairs
	private Map<String, Map<String, String>> sourceGroups; // Stores groupname and attr name & value pairs
	private Map<String, Set<String>> sourceGroupUsers;
	private Table<String, String, String> groupUserTable; // groupname, username, group id
	private int minimumUserId  = 0;
	private int minimumGroupId = 0;
	private String unixPasswordFile;
	private String unixGroupFile;

	private long passwordFileModifiedAt = 0;
	private long groupFileModifiedAt = 0;
	private UgsyncAuditInfo ugsyncAuditInfo;
	private UnixSyncSourceInfo unixSyncSourceInfo;
	private boolean isStartupFlag = false;
	private int deleteCycles;
	private String currentSyncSource;
	private boolean computeDeletes = false;
	Set<String> allGroups = new HashSet<>();


	public static void main(String[] args) throws Throwable {
		UnixUserGroupBuilder ugbuilder = new UnixUserGroupBuilder();
		ugbuilder.init();
		if (LOG.isDebugEnabled()) {
			ugbuilder.print();
		}
	}

	public UnixUserGroupBuilder() {
		isStartupFlag = true;
		minimumUserId = Integer.parseInt(config.getMinUserId());
		minimumGroupId = Integer.parseInt(config.getMinGroupId());
		unixPasswordFile = config.getUnixPasswordFile();
		unixGroupFile = config.getUnixGroupFile();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Minimum UserId: " + minimumUserId + ", minimum GroupId: " + minimumGroupId);
		}

		timeout = config.getUpdateMillisMin();
		enumerateGroupMembers = config.isGroupEnumerateEnabled();

	}

	@Override
	public void init() throws Throwable {
		deleteCycles = 1;

		currentSyncSource = config.getCurrentSyncSource();

		ugsyncAuditInfo = new UgsyncAuditInfo();
		unixSyncSourceInfo = new UnixSyncSourceInfo();
		ugsyncAuditInfo.setSyncSource(currentSyncSource);
		ugsyncAuditInfo.setUnixSyncSourceInfo(unixSyncSourceInfo);
		unixSyncSourceInfo.setFileName(unixPasswordFile);
		unixSyncSourceInfo.setMinUserId(config.getMinUserId());
		unixSyncSourceInfo.setMinGroupId(config.getMinGroupId());
		if (!config.getUnixBackend().equalsIgnoreCase(BACKEND_PASSWD)) {
			useNss = true;
			unixSyncSourceInfo.setUnixBackend("nss");
		} else {
			LOG.warn("DEPRECATED: Unix backend is configured to use /etc/passwd and /etc/group files directly " +
					"instead of standard system mechanisms.");
			unixSyncSourceInfo.setUnixBackend(BACKEND_PASSWD);
		}
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

		if (useNss)
			return System.currentTimeMillis() - lastUpdateTime > timeout;

		long TempPasswordFileModifiedAt = new File(unixPasswordFile).lastModified();
		if (passwordFileModifiedAt != TempPasswordFileModifiedAt) {
			return true;
		}

		long TempGroupFileModifiedAt = new File(unixGroupFile).lastModified();
		return groupFileModifiedAt != TempGroupFileModifiedAt;
	}


	@Override
	public void updateSink(UserGroupSink sink) throws Throwable {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date lastModifiedTime = new Date(passwordFileModifiedAt);
		Date syncTime = new Date(System.currentTimeMillis());
		unixSyncSourceInfo.setLastModified(formatter.format(lastModifiedTime));
		unixSyncSourceInfo.setSyncTime(formatter.format(syncTime));
		if (isChanged() || isStartupFlag) {
			buildUserGroupInfo();
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
			LOG.error("sink.postUserGroupAuditInfo failed with exception: ", t);
		}
		isStartupFlag = false;
	}


	private void buildUserGroupInfo() throws Throwable {
		groupId2groupNameMap = new HashMap<>();
		sourceUsers          = new HashMap<>();
		sourceGroups         = new HashMap<>();
		sourceGroupUsers     = new HashMap<>();
		groupUserTable       = HashBasedTable.create();
		allGroups            = new HashSet<>();

		if (OS.startsWith("Mac")) {
			buildUnixGroupList(MAC_GET_ALL_GROUPS_CMD, MAC_GET_GROUP_CMD, false);
			buildUnixUserList(MAC_GET_ALL_USERS_CMD);
		} else {
			if (!OS.startsWith("Linux")) {
				LOG.warn("Platform not recognized assuming Linux compatible");
			}
			buildUnixGroupList(LINUX_GET_ALL_GROUPS_CMD, LINUX_GET_GROUP_CMD, true);
			buildUnixUserList(LINUX_GET_ALL_USERS_CMD);
		}

		Iterator<String> groupUserTableIterator = groupUserTable.rowKeySet().iterator();
		while (groupUserTableIterator.hasNext()) {
			String groupName                  = groupUserTableIterator.next();
			Map<String, String> groupUsersMap = groupUserTable.row(groupName);
			Set<String> userSet               = new HashSet<>();

			for (String userName : groupUsersMap.keySet()) {
				if (sourceUsers.containsKey(userName)) {
					userSet.add(userName);
				}
			}
			sourceGroupUsers.put(groupName, userSet);
		}

		lastUpdateTime = System.currentTimeMillis();

		if (LOG.isDebugEnabled()) {
			print();
		}
	}

	private void print() {
		for(String user : sourceUsers.keySet()) {
			LOG.debug("USER:" + user);
			Set<String> groups = groupUserTable.column(user).keySet();
			for(String group : groups) {
				LOG.debug("\tGROUP: " + group);
			}
		}
	}

	private void buildUnixUserList(String command) throws Throwable {
		BufferedReader reader = null;
		Map<String, String> userName2uid = new HashMap<>();

		try {
			if (!useNss) {
				File file = new File(unixPasswordFile);
				passwordFileModifiedAt = file.lastModified();
				FileInputStream fis = new FileInputStream(file);
				reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
			} else {
				Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
				reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			}

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;

				String[] tokens = line.split(":");

				int len = tokens.length;

				if (len < 4) {
					LOG.warn("Unable to parse: " + line);
					continue;
				}

				String userName;
				String userId;
				String groupId;

				try {
					userName = tokens[0];
					userId   = tokens[2];
					groupId  = tokens[3];
				}
				catch(ArrayIndexOutOfBoundsException aiobe) {
					LOG.warn("Ignoring line - [" + line + "]: Unable to parse line for getting user information", aiobe);
					continue;
				}

				int numUserId;
				try {
					numUserId = Integer.parseInt(userId);
				} catch (NumberFormatException nfe) {
					LOG.warn("Unix UserId: [" + userId + "]: can not be parsed as valid int. considering as  -1.", nfe);
					numUserId = -1;
				}

				if (numUserId >= minimumUserId) {
					userName2uid.put(userName, userId);
					String groupName = groupId2groupNameMap.get(groupId);
					if (groupName != null) {
						Map<String, String> userAttrMap = new HashMap<>();
						userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
						userAttrMap.put(UgsyncCommonConstants.FULL_NAME, userName);
						userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
						sourceUsers.put(userName, userAttrMap);
						groupUserTable.put(groupName, userName, groupId);
					} else {
						// we are ignoring the possibility that this user was present in getent group.
						LOG.warn("Group Name could not be found for group id: [" + groupId + "]. Skipping adding user [" + userName + "] with id [" + userId + "].");
					}
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Skipping user [" + userName + "] since its userid [" + userId + "] is less than minuserid limit [" + minimumUserId + "].");
					}
				}
			}
		} finally {
			if (reader != null)
				reader.close();
		}

		if (!useNss)
			return;

		// this does a reverse check as not all users might be listed in getent passwd
		if (enumerateGroupMembers) {
			String line;
			Table<String, String, String> tempGroupUserTable = HashBasedTable.create();

			if (LOG.isDebugEnabled()) {
				LOG.debug("Start drill down group members");
			}
			for (String userName : groupUserTable.columnKeySet()) {

				if (sourceUsers.containsKey(userName))// skip users we already now about
					continue;

				if (LOG.isDebugEnabled()) {
					LOG.debug("Enumerating user " + userName);
				}

				// "id" is same across Linux / BSD / MacOSX
				// gids are used as id might return groups with spaces, ie "domain users"
				Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", "id -G " + userName});

				try {
					reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					line = reader.readLine();
				} finally {
					reader.close();
				}

				if (LOG.isDebugEnabled()) {
					LOG.debug("id -G returned " + line);
				}

				if (line == null || line.trim().isEmpty()) {
					LOG.warn("User " + userName + " could not be resolved");
					continue;
				}

				// check if all groups returned by id are visible to ranger
				for (String gid : line.split(" ")) {
					if (Integer.parseInt(gid) >= minimumGroupId) {
						String groupName = groupId2groupNameMap.get(gid);
						if (groupName != null) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("New group user mapping found : " + groupName + " " + userName);
							}
							tempGroupUserTable.put(groupName, userName, gid);
						}
					}
				}

				Map<String, String> userAttrMap = new HashMap<>();
				userAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, userName);
				userAttrMap.put(UgsyncCommonConstants.FULL_NAME, userName);
				userAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
				sourceUsers.put(userName, userAttrMap);
			}
			groupUserTable.putAll(tempGroupUserTable);
			if (LOG.isDebugEnabled()) {
				LOG.debug("End drill down group members");
			}
		}
	}

	private void parseMembers(String line) {
		if (line == null || line.isEmpty())
			return;

		String[] tokens = line.split(":");

		if (tokens.length < 3)
			return;

		String groupName = tokens[0];
		String groupId = tokens[2];
		String groupMembers = null;

		if (tokens.length > 3)
			groupMembers = tokens[3];

		groupId2groupNameMap.remove(groupId);

		int numGroupId = Integer.parseInt(groupId);
		if (numGroupId < minimumGroupId)
			return;

		groupId2groupNameMap.put(groupId, groupName);
		Map<String, String> groupAttrMap = new HashMap<>();
		groupAttrMap.put(UgsyncCommonConstants.ORIGINAL_NAME, groupName);
		groupAttrMap.put(UgsyncCommonConstants.FULL_NAME, groupName);
		groupAttrMap.put(UgsyncCommonConstants.SYNC_SOURCE, currentSyncSource);
		sourceGroups.put(groupName, groupAttrMap);

		if (groupMembers != null && !groupMembers.trim().isEmpty()) {
			for (String user : groupMembers.split(",")) {
				groupUserTable.put(groupName, user, groupId);
			}
		} else {
			sourceGroupUsers.put(groupName, new HashSet<String>());
		}
	}

	private void buildUnixGroupList(String allGroupsCmd, String groupCmd, boolean useGid) throws Throwable {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Start enumerating groups");
		}
		BufferedReader reader = null;

		try {
			if (!useNss) {
				File file = new File(unixGroupFile);
				groupFileModifiedAt = file.lastModified();
				FileInputStream fis = new FileInputStream(file);
				reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
			} else {
				Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", allGroupsCmd});
				reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
			}

			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.trim().isEmpty())
					parseMembers(line);
			}
		} finally {
			if (reader != null)
				reader.close();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("End enumerating group");
		}

		if (!useNss)
			return;

		if (enumerateGroupMembers) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Start enumerating group members");
			}
			String line = null;
			Map<String,String> copy = new HashMap<String, String>(groupId2groupNameMap);

			for (Map.Entry<String, String> group : copy.entrySet()) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Enumerating group: " + group.getValue() + " GID(" + group.getKey() + ")");
				}

				String command;
				if (useGid) {
					command = String.format(groupCmd, group.getKey());
				} else {
					command = String.format(groupCmd, group.getValue());
				}

				String[] cmd = new String[]{"bash", "-c", command + " " + group.getKey()};
				if (LOG.isDebugEnabled()) {
					LOG.debug("Executing: " + Arrays.toString(cmd));
				}

				try {
					Process process = Runtime.getRuntime().exec(cmd);
					reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					line = reader.readLine();
				} finally {
					if (reader != null)
						reader.close();
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("bash -c " + command + " for group " + group + " returned " + line);
				}

				parseMembers(line);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("End enumerating group members");
			}
		}

		if (config.getEnumerateGroups() != null) {
			String line;
			String[] groups = config.getEnumerateGroups().split(",");

			if (LOG.isDebugEnabled()) {
				LOG.debug("Adding extra groups");
			}

			for (String group : groups) {
				String command = String.format(groupCmd, group);
				String[] cmd = new String[]{"bash", "-c", command};
				if (LOG.isDebugEnabled()) {
					LOG.debug("Executing: " + Arrays.toString(cmd));
				}

				try {
					Process process = Runtime.getRuntime().exec(cmd);
					reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					line = reader.readLine();
				} finally {
					if (reader != null)
						reader.close();
				}

				if (LOG.isDebugEnabled()) {
					LOG.debug("bash -c " + command + " for group " + group + " returned " + line);
				}

				parseMembers(line);
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Done adding extra groups");
			}
		}
	}

	@VisibleForTesting
	Map<String, Set<String>> getGroupUserListMap() {
		return sourceGroupUsers;
	}

	@VisibleForTesting
	Map<String,String> getGroupId2groupNameMap() {
		return groupId2groupNameMap;
	}

	@VisibleForTesting
	Set<String> getUsers() {
		return sourceUsers.keySet();
	}

}
