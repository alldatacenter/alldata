/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.hdfs;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.server.namenode.INode;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributeProvider.AccessControlEnforcer;
import org.apache.hadoop.hdfs.server.namenode.INodeAttributes;
import org.apache.hadoop.hdfs.server.namenode.snapshot.Snapshot;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Direct tests for the {@link RangerHdfsAuthorizer} without going through the HDFS layer.
 *
 */
public class RangerHdfsAuthorizerTest {

    private final static int SNAPSHOT_ID = Snapshot.CURRENT_STATE_ID;
    private final static String FILE_OWNER = "fileOwner";
    private final static String FILE_GROUP = "superGroup";
    private static final FsPermission READ_ONLY = new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE);
    private static final FsPermission EXEC_BY_OWNER = new FsPermission(FsAction.EXECUTE, FsAction.NONE, FsAction.NONE);

    private static RangerHdfsAuthorizer authorizer;
    private static AccessControlEnforcer rangerControlEnforcer;

    static class TestFileSystem {
        final String path;
        final String[] pathSegments;
        final INode[] nodes;
        final INodeAttributes[] attributes;
        final int ancestorIndex;

        TestFileSystem(String path) {
            this.path = path;
            pathSegments = path.split("/");
            nodes = new INode[pathSegments.length];
            attributes = new INode[pathSegments.length];
            for (int i = 0; i < pathSegments.length; i++) {
                boolean file = i == pathSegments.length - 1;
                INode node = createNode(pathSegments, i, FILE_OWNER, FILE_GROUP, file);
                nodes[i] = node;
                attributes[i] = node;
            }
            ancestorIndex = nodes.length - 2;
        }

        public void setFilePermission(FsPermission fsPermission) {
            when(nodes[nodes.length-1].getFsPermission()).thenReturn(fsPermission);
        }

        public void setParentDirectoryPermission(FsPermission fsPermission) {
            when(nodes[nodes.length-2].getFsPermission()).thenReturn(fsPermission);
        }

        /**
         * Checks that the <b>directory</b> access is <b>allowed</b> for the given user in the given groups.
         * Throws an exception, if not.
         */
        public void checkDirAccess(FsAction access, String userName, String... groups) throws AccessControlException {
            final UserGroupInformation user = UserGroupInformation.createUserForTesting(userName, groups);

            INodeAttributeProvider.AuthorizationContext.Builder builder =
                    new  INodeAttributeProvider.AuthorizationContext.Builder()
                    .fsOwner(FILE_OWNER)
                    .supergroup(FILE_GROUP)
                    .callerUgi(user)
                    .inodeAttrs(Arrays.copyOf(attributes, attributes.length - 1))
                    .inodes(Arrays.copyOf(nodes, nodes.length - 1))
                    .pathByNameArr(new byte[0][0])
                    .snapshotId(SNAPSHOT_ID)
                    .path(path)
                    .ancestorIndex(ancestorIndex - 1)
                    .doCheckOwner(false)
                    .ancestorAccess(null)
                    .parentAccess(null)
                    .access(access)
                    .subAccess(null)
                    .ignoreEmptyDir(false)
                    .operationName(null)
                    .callerContext(null);

            INodeAttributeProvider.AuthorizationContext authorizationContext
                    = new INodeAttributeProvider.AuthorizationContext(builder);

            rangerControlEnforcer.checkPermissionWithContext(authorizationContext);
        }

        /**
         * Checks that the <b>file</b> access is <b>allowed</b> for the given user in the given groups.
         * Throws an exception, if not.
         */
        public void checkAccess(FsAction access, String userName, String... groups) throws AccessControlException {
            final UserGroupInformation user = UserGroupInformation.createUserForTesting(userName, groups);

            INodeAttributeProvider.AuthorizationContext.Builder builder =
                    new  INodeAttributeProvider.AuthorizationContext.Builder()
                            .fsOwner(FILE_OWNER)
                            .supergroup(FILE_GROUP)
                            .callerUgi(user)
                            .inodeAttrs(attributes)
                            .inodes(nodes)
                            .pathByNameArr(new byte[0][0])
                            .snapshotId(SNAPSHOT_ID)
                            .path(path)
                            .ancestorIndex(ancestorIndex - 1)
                            .doCheckOwner(false)
                            .ancestorAccess(null)
                            .parentAccess(null)
                            .access(access)
                            .subAccess(null)
                            .ignoreEmptyDir(false)
                            .operationName(null)
                            .callerContext(null);

            INodeAttributeProvider.AuthorizationContext authorizationContext
                    = new INodeAttributeProvider.AuthorizationContext(builder);

            rangerControlEnforcer.checkPermissionWithContext(authorizationContext);
        }

        /**
         * Checks that the <b>file</b> access is <b>blocked</b> for the given user in the given groups.
         * Throws an exception, if not.
         */
        public void checkAccessBlocked(FsAction access, String userName, String... groups)
                throws AccessControlException {
            try {
                checkAccess(access, userName, groups);
                Assert.fail("Access should be blocked for " + path + " access=" + access + " for user=" + userName
                        + " groups=" + Arrays.asList(groups));
            } catch (AccessControlException ace) {
                Assert.assertNotNull(ace);
            }
        }

        /**
         * Checks that the <b>directory</b> access is <b>blocked</b> for the given user in the given groups.
         * Throws an exception, if not.
         */
        public void checkDirAccessBlocked(FsAction access, String userName, String... groups)
                throws AccessControlException {
            try {
                checkDirAccess(access, userName, groups);
                Assert.fail("Access should be blocked for parent directory of " + path + " access=" + access
                        + " for user=" + userName + " groups=" + Arrays.asList(groups));
            } catch (AccessControlException ace) {
                Assert.assertNotNull(ace);
            }
        }

    }

    @BeforeClass
    public static void setup() {
        try {
            File file = File.createTempFile("hdfs-version-site", ".xml");
            file.deleteOnExit();

            try(final FileOutputStream outStream = new FileOutputStream(file);
                final OutputStreamWriter writer = new OutputStreamWriter(outStream, StandardCharsets.UTF_8)) {
                writer.write("<configuration>\n" +
                        "        <property>\n" +
                        "                <name>hdfs.version</name>\n" +
                        "                <value>hdfs_version_3.0</value>\n" +
                        "        </property>\n" +
                        "        <property>\n" +
                        "                <name>xasecure.add-hadoop-authorization</name>\n" +
                        "                <value>true</value>\n" +
                        "        </property>\n" +
                        "</configuration>\n");
            }

            authorizer = new RangerHdfsAuthorizer(new org.apache.hadoop.fs.Path(file.toURI()));
            authorizer.start();
        } catch (Exception exception) {
            Assert.fail("Cannot create hdfs-version-site file:[" + exception.getMessage() + "]");
        }

        AccessControlEnforcer accessControlEnforcer = null;
        rangerControlEnforcer = authorizer.getExternalAccessControlEnforcer(accessControlEnforcer);
    }

    @AfterClass
    public static void teardown() {
        authorizer.stop();
    }

    @Test
    public void testAccessControlEnforcer() {
        Assert.assertNotNull("rangerControlEnforcer", rangerControlEnforcer);
    }

    @Test
    public void readTest() throws AccessControlException {
        readPath("/tmp/tmpdir/data-file2");
    }

    @Test
    public void readTestUsingTagPolicy() throws Exception {
        final TestFileSystem fs = new TestFileSystem("/tmp/tmpdir6/data-file2");
        fs.setFilePermission(READ_ONLY);

        // Try to read the files as "bob" - this should be allowed (by the policy - user)
        fs.checkAccess(null,  "bob"); // traverse check
        fs.checkAccess(FsAction.READ, "bob"); // read check

        // Now try to read the file as "alice" - this should be allowed (by the policy - group)
        fs.checkAccess(null, "alice", "IT"); // traverse check
        fs.checkAccess(FsAction.READ, "alice", "IT"); // read check

        // Now try to read the file as unknown user "eve" - this should not be allowed
        fs.checkAccess(null, "eve"); // traverse check
        fs.checkAccessBlocked(FsAction.READ, "eve"); // read deny check to public

        // Now try to read the file as user "dave" - this should not be allowed
        fs.checkAccess(null, "dave"); // traverse check
        fs.checkAccessBlocked(FsAction.READ, "dave"); // read deny check for public
    }

    @Test
    public void writeTest() throws AccessControlException {
        final TestFileSystem fs = new TestFileSystem("/tmp/tmpdir2/data-file3");

        // Try to write to the file as "bob" - this should be allowed (by the policy - user)
        fs.checkAccess(null, "bob"); // traverse check
        fs.checkAccess(FsAction.WRITE, "bob"); // write check

        // Now try to write to the file as "alice" - this should be allowed (by the policy - group)
        fs.checkAccess(null, "alice", "IT"); // traverse check
        fs.checkAccess(FsAction.WRITE, "alice", "IT"); // write check

        // Now try to write the file as unknown user "eve" - this should not be allowed
        fs.checkAccessBlocked(null, "eve"); // traverse check
        fs.checkAccessBlocked(FsAction.WRITE, "eve"); // write deny check for public
    }

    @Test
    public void executeTest() throws AccessControlException {
        final TestFileSystem fs = new TestFileSystem("/tmp/tmpdir3/data-file2");
        fs.setFilePermission(READ_ONLY);
        fs.setParentDirectoryPermission(EXEC_BY_OWNER);

        // Try to list the files as "bob" - this should be allowed (by the policy - user)
        fs.checkDirAccess(null,  "bob"); // traverse check
        fs.checkDirAccess(FsAction.READ_EXECUTE, "bob"); // dir list check

        // Try to list the directory as "alice" - this should be allowed (by the policy - group)
        fs.checkDirAccess(null, "alice", "IT"); // traverse check
        fs.checkDirAccess(FsAction.READ_EXECUTE, "alice", "IT"); // dir list check

        // Now try to list the directory as unknown user "eve" - this should not be allowed
        fs.checkDirAccessBlocked(null, "eve"); // traverse check
        fs.checkDirAccessBlocked(FsAction.READ_EXECUTE, "eve"); // dir list deny check for public

    }

    @Test
    public void HDFSFileNameTokenReadTest() throws AccessControlException {
        readPath("/tmp/tmpdir4/data-file");
        readFailWithPath("/tmp/tmpdir4/t/abc");
    }

    @Test
    public void HDFSBaseFileNameTokenReadTest() throws AccessControlException {
        readPath("/tmp/tmpdir5/data-file.txt");
        readFailWithPath("/tmp/tmpdir5/data-file.csv");
        readFailWithPath("/tmp/tmpdir5/t/data-file.txt");
    }

    private void readFailWithPath(String path) throws AccessControlException {
        final TestFileSystem fs = new TestFileSystem(path);
        fs.setFilePermission(READ_ONLY);

        // Now try to read the file as "bob" - this should NOT be allowed
        fs.checkAccessBlocked(FsAction.READ, "bob"); // read check

        // Now try to read the file as "alice" - this should NOT be allowed
        fs.checkAccessBlocked(FsAction.READ, "alice", "IT"); // read check

        // Now try to read the file as unknown user "eve" - this should not be allowed
        fs.checkAccessBlocked(FsAction.READ, "eve", "IT"); // read deny check for public
    }

    private void readPath(String path) throws AccessControlException {
        final TestFileSystem fs = new TestFileSystem(path);
        fs.setFilePermission(READ_ONLY);

        // Try to read the files as "bob" - this should be allowed (by the policy - user)
        fs.checkAccess(null,  "bob"); // traverse check
        fs.checkAccess(FsAction.READ, "bob"); // read check

        // Now try to read the file as "alice" - this should be allowed (by the policy - group)
        fs.checkAccess(null, "alice", "IT"); // traverse check
        fs.checkAccess(FsAction.READ, "alice", "IT"); // read check

        // Now try to read the file as unknown user "eve" - this should not be allowed
        fs.checkAccessBlocked(null, "eve"); // traverse deny check for public
        fs.checkAccessBlocked(FsAction.READ, "eve"); // read deny check for public
    }

    private static INode createNode(String[] pathSegments, int i, String owner, String group, boolean file) {
        String fullPath = StringUtils.join(pathSegments, '/', 0, i+1);
        String name = pathSegments[i];
        INode mock = Mockito.mock(INode.class);
        try {
            when(mock.getLocalNameBytes()).thenReturn(name.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        when(mock.getUserName()).thenReturn(owner);
        when(mock.getGroupName()).thenReturn(group);
        when(mock.getFullPathName()).thenReturn(fullPath);
        when(mock.isFile()).thenReturn(file);
        when(mock.isDirectory()).thenReturn(!file);
        when(mock.toString()).thenReturn(name);
        return mock;
    }
}
