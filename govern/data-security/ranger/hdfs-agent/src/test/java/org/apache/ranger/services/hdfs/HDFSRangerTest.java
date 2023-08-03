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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.PrivilegedExceptionAction;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer;
import org.junit.Assert;

/**
 * Here we plug the Ranger AccessControlEnforcer into HDFS.
 *
 * A custom RangerAdminClient is plugged into Ranger in turn, which loads security policies from a local file. These policies were
 * generated in the Ranger Admin UI for a service called "HDFSTest". It contains three policies, each of which grants read, write and
 * execute permissions in turn to "/tmp/tmpdir", "/tmp/tmpdir2" and "/tmp/tmpdir3" to a user called "bob" and to a group called "IT".
 *
 * In addition we have a TAG based policy, which grants "read" access to "bob" and the "IT" group to "/tmp/tmpdir6" (which is associated
 * with the tag called "TmpdirTag". A "hdfs_path" entity was created in Apache Atlas + then associated with the "TmpdirTag". This was
 * then imported into Ranger using the TagSyncService. The policies were then downloaded locally and saved for testing off-line.
 */
public class HDFSRangerTest {

    private static final File baseDir = new File("./target/hdfs/").getAbsoluteFile();
    private static MiniDFSCluster hdfsCluster;
    private static String defaultFs;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        Configuration conf = new Configuration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        conf.set("dfs.namenode.inode.attributes.provider.class", RangerHdfsAuthorizer.class.getName());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();
        defaultFs = conf.get("fs.defaultFS");
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        FileUtil.fullyDelete(baseDir);
        hdfsCluster.shutdown();
    }

    @org.junit.Test
    public void readTest() throws Exception {
        HDFSReadTest("/tmp/tmpdir/data-file2");
    }

    @org.junit.Test
    public void writeTest() throws Exception {

        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path("/tmp/tmpdir2/data-file3");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Now try to write to the file as "bob" - this should be allowed (by the policy - user)
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Write to the file
                fs.append(file);

                fs.close();
                return null;
            }
        });

        // Now try to write to the file as "alice" - this should be allowed (by the policy - group)
        ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Write to the file
                fs.append(file);

                fs.close();
                return null;
            }
        });

        // Now try to read the file as unknown user "eve" - this should not be allowed
        ugi = UserGroupInformation.createUserForTesting("eve", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Write to the file
                try {
                    fs.append(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });
    }

    @org.junit.Test
    public void executeTest() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path("/tmp/tmpdir3/data-file2");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));

        // Change the parent directory permissions to be execute only for the owner
        Path parentDir = new Path("/tmp/tmpdir3");
        fileSystem.setPermission(parentDir, new FsPermission(FsAction.EXECUTE, FsAction.NONE, FsAction.NONE));

        // Try to read the directory as "bob" - this should be allowed (by the policy - user)
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                RemoteIterator<LocatedFileStatus> iter = fs.listFiles(file.getParent(), false);
                Assert.assertTrue(iter.hasNext());

                fs.close();
                return null;
            }
        });

        // Try to read the directory as "alice" - this should be allowed (by the policy - group)
        ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                RemoteIterator<LocatedFileStatus> iter = fs.listFiles(file.getParent(), false);
                Assert.assertTrue(iter.hasNext());

                fs.close();
                return null;
            }
        });

        // Now try to read the directory as unknown user "eve" - this should not be allowed
        ugi = UserGroupInformation.createUserForTesting("eve", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Write to the file
                try {
                    RemoteIterator<LocatedFileStatus> iter = fs.listFiles(file.getParent(), false);
                    Assert.assertTrue(iter.hasNext());
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });

    }

    @org.junit.Test
    public void readTestUsingTagPolicy() throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path("/tmp/tmpdir6/data-file2");
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));

        // Now try to read the file as "bob" - this should be allowed (by the policy - user)
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                String content = new String(output.toByteArray());
                Assert.assertTrue(content.startsWith("data0"));

                fs.close();
                return null;
            }
        });

        // Now try to read the file as "alice" - this should be allowed (by the policy - group)
        ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                String content = new String(output.toByteArray());
                Assert.assertTrue(content.startsWith("data0"));

                fs.close();
                return null;
            }
        });

        // Now try to read the file as unknown user "eve" - this should not be allowed
        ugi = UserGroupInformation.createUserForTesting("eve", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });

        // Now try to read the file as known user "dave" - this should not be allowed, as he doesn't have the correct permissions
        ugi = UserGroupInformation.createUserForTesting("dave", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });
    }

    @org.junit.Test
    public void HDFSFileNameTokenReadTest() throws Exception {
        HDFSReadTest("/tmp/tmpdir4/data-file");
        HDFSReadFailTest("/tmp/tmpdir4/t/abc");
    }

    @org.junit.Test
    public void HDFSBaseFileNameTokenReadTest() throws Exception {
        HDFSReadTest("/tmp/tmpdir5/data-file.txt");
        HDFSReadFailTest("/tmp/tmpdir5/data-file.csv");
        HDFSReadFailTest("/tmp/tmpdir5/t/data-file.txt");
    }

    // TODO
    @org.junit.Test
    @org.junit.Ignore
    public void HDFSContentSummaryTest() throws Exception {
        HDFSGetContentSummary("/tmp/get-content-summary");
    }

    void HDFSReadTest(String fileName) throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path(fileName);
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));

        // Now try to read the file as "bob" - this should be allowed (by the policy - user)
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                String content = new String(output.toByteArray());
                Assert.assertTrue(content.startsWith("data0"));

                fs.close();
                return null;
            }
        });

        // Now try to read the file as "alice" - this should be allowed (by the policy - group)
        ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                FSDataInputStream in = fs.open(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copy(in, output);
                String content = new String(output.toByteArray());
                Assert.assertTrue(content.startsWith("data0"));

                fs.close();
                return null;
            }
        });

        // Now try to read the file as unknown user "eve" - this should not be allowed
        ugi = UserGroupInformation.createUserForTesting("eve", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });
    }
    void HDFSReadFailTest(String fileName) throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        final Path file = new Path(fileName);
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));

        // Now try to read the file as "bob" - this should NOT be allowed
        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });

        // Now try to read the file as "alice" - this should NOT be allowed
        ugi = UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });

        // Now try to read the file as unknown user "eve" - this should not be allowed
        ugi = UserGroupInformation.createUserForTesting("eve", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                // Read the file
                try {
                    fs.open(file);
                    Assert.fail("Failure expected on an incorrect permission");
                } catch (AccessControlException ex) {
                    // expected
                    Assert.assertTrue(AccessControlException.class.getName().equals(ex.getClass().getName()));
                }

                fs.close();
                return null;
            }
        });
    }

    void HDFSGetContentSummary(final String dirName) throws Exception {

        String subdirName = dirName + "/tmpdir";

        createFile(subdirName, 1);
        createFile(subdirName, 2);

        UserGroupInformation ugi = UserGroupInformation.createUserForTesting("bob", new String[] {});
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                Configuration conf = new Configuration();
                conf.set("fs.defaultFS", defaultFs);

                FileSystem fs = FileSystem.get(conf);

                try {
                    // GetContentSummary on the directory dirName
                    ContentSummary contentSummary = fs.getContentSummary(new Path(dirName));

                    long directoryCount = contentSummary.getDirectoryCount();
                    Assert.assertTrue("Found unexpected number of directories; expected-count=3, actual-count=" + directoryCount, directoryCount == 3);
                } catch (Exception e) {
                    Assert.fail("Failed to getContentSummary, exception=" + e);
                }
                fs.close();
                return null;
            }
        });
    }

    void createFile(String baseDir, Integer index) throws Exception {
        FileSystem fileSystem = hdfsCluster.getFileSystem();

        // Write a file - the AccessControlEnforcer won't be invoked as we are the "superuser"
        String dirName = baseDir + (index != null ? String.valueOf(index) : "");
        String fileName = dirName + "/dummy-data";
        final Path file = new Path(fileName);
        FSDataOutputStream out = fileSystem.create(file);
        for (int i = 0; i < 1024; ++i) {
            out.write(("data" + i + "\n").getBytes("UTF-8"));
            out.flush();
        }
        out.close();

        // Change permissions to read-only
        fileSystem.setPermission(file, new FsPermission(FsAction.READ, FsAction.NONE, FsAction.NONE));
    }
}
