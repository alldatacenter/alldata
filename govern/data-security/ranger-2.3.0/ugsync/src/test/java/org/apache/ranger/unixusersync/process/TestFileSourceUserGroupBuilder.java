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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.apache.ranger.unixusersync.config.UserGroupSyncConfig;
import org.apache.ranger.usergroupsync.PolicyMgrUserGroupBuilderTest;
import org.junit.Test;

public class TestFileSourceUserGroupBuilder {

    private UserGroupSyncConfig config = UserGroupSyncConfig.getInstance();

    @Test
    public void testUpdateSinkFromCsvFile() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC, "src/test/resources/usergroups.csv");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(4, sink.getTotalUsers());
        assertEquals(2, sink.getTotalGroups());

        assertTrue(sink.getAllUsers().contains("user1"));
        assertTrue(sink.getAllUsers().contains("user2"));
        assertTrue(sink.getAllUsers().contains("user3"));
        assertTrue(sink.getAllUsers().contains("user4"));

        assertTrue(sink.getAllGroups().contains("group1"));
        assertTrue(sink.getAllGroups().contains("group2"));
    }

    @Test
    public void testUpdateSinkFromCsvFileWithCustomDelimiter() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC,
                "src/test/resources/usergroups-other-delim.csv");
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_DELIMITER, "|");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(4, sink.getTotalUsers());
        assertEquals(2, sink.getTotalGroups());

        assertTrue(sink.getAllUsers().contains("user1"));
        assertTrue(sink.getAllUsers().contains("user2"));
        assertTrue(sink.getAllUsers().contains("user3"));
        assertTrue(sink.getAllUsers().contains("user4"));

        assertTrue(sink.getAllGroups().contains("group1"));
        assertTrue(sink.getAllGroups().contains("group2"));
    }

    @Test
    public void testUpdateSinkFromCsvFileMisSpelledDelimiterProperty() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC,
                "src/test/resources/usergroups-other-delim.csv");
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_DELIMITERER, "|");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(4, sink.getTotalUsers());
        assertEquals(2, sink.getTotalGroups());

        assertTrue(sink.getAllUsers().contains("user1"));
        assertTrue(sink.getAllUsers().contains("user2"));
        assertTrue(sink.getAllUsers().contains("user3"));
        assertTrue(sink.getAllUsers().contains("user4"));

        assertTrue(sink.getAllGroups().contains("group1"));
        assertTrue(sink.getAllGroups().contains("group2"));
    }

    @Test
    public void testUpdateSinkFromJsonFile() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC, "src/test/resources/usergroups.json");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(4, sink.getTotalUsers());
        assertEquals(7, sink.getTotalGroups());

        assertTrue(sink.getAllUsers().contains("user1"));
        assertTrue(sink.getAllUsers().contains("user2"));
        assertTrue(sink.getAllUsers().contains("user3"));
        assertTrue(sink.getAllUsers().contains("user4"));

        assertTrue(sink.getAllGroups().contains("group1"));
        assertTrue(sink.getAllGroups().contains("group2"));
        assertTrue(sink.getAllGroups().contains("group3"));
        assertTrue(sink.getAllGroups().contains("group4"));
        assertTrue(sink.getAllGroups().contains("group5"));
        assertTrue(sink.getAllGroups().contains("group6"));
        assertTrue(sink.getAllGroups().contains("group7"));
    }

    @Test
    public void testUpdateSinkWithUserAndGroupMapping() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC, "src/test/resources/usergroups-dns.csv");
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_DELIMITERER, "|");

        config.setProperty(UserGroupSyncConfig.SYNC_MAPPING_USERNAME, "s/[=]/_/g");
        config.setProperty(UserGroupSyncConfig.SYNC_MAPPING_USERNAME + ".1", "s/[,]//g");

        config.setProperty(UserGroupSyncConfig.SYNC_MAPPING_GROUPNAME, "s/[=]//g");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(4, sink.getTotalUsers());
        assertEquals(2, sink.getTotalGroups());

        assertTrue(sink.getAllUsers().contains("CN_User1 OU_Org1 O_Apache L_Santa Monica ST_CA C_US"));
        assertTrue(sink.getAllUsers().contains("CN_User2 OU_Org1 O_Apache L_Santa Monica ST_CA C_US"));
        assertTrue(sink.getAllUsers().contains("CN_User3 OU_Org1 O_Apache L_Santa Monica ST_CA C_US"));
        assertTrue(sink.getAllUsers().contains("CN_User4 OU_Org1 O_Apache L_Santa Monica ST_CA C_US"));

        assertTrue(sink.getAllGroups().contains("group1"));
        assertTrue(sink.getAllGroups().contains("group2"));
    }

    @Test
    public void testUpdateSinkDisableInvalidNameCheck() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC, "src/test/resources/usergroups-special-characters.csv");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        fileBuilder.updateSink(sink);

        assertEquals(0, sink.getTotalInvalidGroups());
        assertEquals(0, sink.getTotalInvalidUsers());
    }

    @Test
    public void testUpdateSinkEnableInvalidNameCheck() throws Throwable {
        config.setProperty(UserGroupSyncConfig.UGSYNC_SOURCE_FILE_PROC, "src/test/resources/usergroups-special-characters.csv");

        FileSourceUserGroupBuilder fileBuilder = new FileSourceUserGroupBuilder();
        fileBuilder.init();

        PolicyMgrUserGroupBuilderTest sink = new PolicyMgrUserGroupBuilderTest();
        sink.init();
        sink.setUserSyncNameValidationEnabled("true");
        fileBuilder.updateSink(sink);

        assertEquals(2, sink.getTotalInvalidGroups());
        assertEquals(1, sink.getTotalInvalidUsers());
    }
}
