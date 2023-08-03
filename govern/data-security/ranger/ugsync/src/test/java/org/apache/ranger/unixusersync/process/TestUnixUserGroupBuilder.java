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

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasValue;

public class TestUnixUserGroupBuilder {
    private UserGroupSyncConfig config;

    @Before
    public void setUp() {
        config = UserGroupSyncConfig.getInstance();
        config.setProperty("ranger.usersync.unix.minUserId", "0");
        config.setProperty("ranger.usersync.unix.minGroupId", "0");
    }

    @Test
    public void testBuilderPasswd() throws Throwable {
        config.setProperty("ranger.usersync.unix.backend", "passwd");
        config.setProperty(UserGroupSyncConfig.UGSYNC_UNIX_PASSWORD_FILE, "/etc/passwd");
    	config.setProperty(UserGroupSyncConfig.UGSYNC_UNIX_GROUP_FILE, "/etc/group");


        UnixUserGroupBuilder builder = new UnixUserGroupBuilder();
        builder.init();

        Map<String, String> groups = builder.getGroupId2groupNameMap();
        String name = groups.get("0");
        assertThat(name, anyOf(equalTo("wheel"), equalTo("root")));

        Map<String, Set<String>> groupUsers = builder.getGroupUserListMap();
        Set<String> users = groupUsers.get(name);
        assertNotNull(users);
        assertThat(users, anyOf(hasItem("wheel"), hasItem("root")));

    }

    @Test
    public void testBuilderNss() throws Throwable {
        config.setProperty("ranger.usersync.unix.backend", "nss");

        UnixUserGroupBuilder builder = new UnixUserGroupBuilder();
        builder.init();

        Map<String, String> groups = builder.getGroupId2groupNameMap();
        String name = groups.get("0");
        assertThat(name, anyOf(equalTo("wheel"), equalTo("root")));

        Map<String, Set<String>> groupUsers = builder.getGroupUserListMap();
        Set<String> users = groupUsers.get(name);
        assertNotNull(users);
        assertThat(users, anyOf(hasItem("wheel"), hasItem("root")));
    }

    @Test
    public void testBuilderExtraGroups() throws Throwable {
        config.setProperty("ranger.usersync.unix.backend", "nss");
        config.setProperty("ranger.usersync.group.enumerategroup", "root,wheel,daemon");

        UnixUserGroupBuilder builder = new UnixUserGroupBuilder();
        builder.init();

        // this is not a full test as it cannot be mocked sufficiently
        Map<String, String> groups = builder.getGroupId2groupNameMap();
        assertTrue(groups.containsValue("daemon"));
        assertThat(groups, anyOf(hasValue("wheel"), hasValue("root")));
    }

    @Test
    public void testMinUidGid() throws Throwable {
        config.setProperty("ranger.usersync.unix.backend", "nss");
        config.setProperty("ranger.usersync.unix.minUserId", "500");
        config.setProperty("ranger.usersync.unix.minGroupId", "500");

        UnixUserGroupBuilder builder = new UnixUserGroupBuilder();
        builder.init();

        // this is not a full test as it cannot be mocked sufficiently
        Map<String, String> groups = builder.getGroupId2groupNameMap();
        assertFalse(groups.containsValue("wheel"));

        Map<String, Set<String>> groupUsers = builder.getGroupUserListMap();
        assertNull(groupUsers.get("wheel"));
    }
    
    @Test
    public void testUnixPasswdAndGroupFile() throws Throwable {
    	config.setProperty("ranger.usersync.unix.backend", "passwd");
    	config.setProperty(UserGroupSyncConfig.UGSYNC_UNIX_PASSWORD_FILE, "src/test/resources/passwordFile.txt");
    	config.setProperty(UserGroupSyncConfig.UGSYNC_UNIX_GROUP_FILE, "src/test/resources/groupFile.txt");

        UnixUserGroupBuilder builder = new UnixUserGroupBuilder();
        builder.init();

        Map<String, String> groups = builder.getGroupId2groupNameMap();
        String name = groups.get("1028");
        assertThat(name, anyOf(equalTo("wheel"), equalTo("sam")));

        Map<String, Set<String>> groupUsers = builder.getGroupUserListMap();
        Set<String> users = groupUsers.get("sam");
        assertNotNull(groupUsers);
        assertThat(users, anyOf(hasItem("wheel"), hasItem("sam")));

    }

}
