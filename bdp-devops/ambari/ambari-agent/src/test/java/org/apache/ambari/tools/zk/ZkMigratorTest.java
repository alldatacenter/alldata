/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.tools.zk;

import static org.apache.zookeeper.ZooDefs.Perms.DELETE;
import static org.apache.zookeeper.ZooDefs.Perms.READ;
import static org.apache.zookeeper.ZooDefs.Perms.WRITE;
import static org.apache.zookeeper.ZooDefs.Perms.ALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ category.SlowTest.class})
public class ZkMigratorTest {
  private CuratorFramework cli;
  private TestingServer zkTestServer;

  @Test
  public void testSetAclsOnSingleNode() throws Exception {
    // Given
    path("/single");
    // When
    setAcls("/single", "ip:127.0.0.1:rwd");
    // Then
    assertHasAcl("/single", "ip", "127.0.0.1", WRITE | READ | DELETE);
  }

  @Test
  public void testSetAclsOnParentAndItsDirectChildren() throws Exception {
    // Given
    path("/parent");
    path("/parent/a");
    path("/parent/b");
    // When
    setAcls("/parent", "ip:127.0.0.1:rd");
    // Then
    assertHasAcl("/parent", "ip", "127.0.0.1", READ | DELETE);
    assertHasAcl("/parent/a", "ip", "127.0.0.1", READ | DELETE);
    assertHasAcl("/parent/b", "ip", "127.0.0.1", READ | DELETE);
  }

  @Test
  public void testDeleteRecursive() throws Exception {
    // Given
    path("/parent");
    path("/parent/a");
    path("/parent/b");
    path("/parent/b/q");
    // When
    deleteZnode("/parent");
    // Then
    assertRemoved("/parent");
    assertRemoved("/parent/a");
    assertRemoved("/parent/b");
    assertRemoved("/parent/b/q");
  }

  @Test
  public void testDeleteRecursiveWildcard() throws Exception {
    // Given
    path("/parent");
    path("/parent/a");
    path("/parent/b");
    path("/parent/b/q");
    // When
    deleteZnode("/parent/*");
    // Then
    assertHasNode("/parent");
    assertRemoved("/parent/a");
    assertRemoved("/parent/b");
    assertRemoved("/parent/b/q");
  }

  @Test
  public void testSetAclsRecursively() throws Exception {
    // Given
    path("/parent");
    path("/parent/a");
    path("/parent/a/b");
    path("/parent/a/b/c");
    // When
    setAcls("/", "ip:127.0.0.1:r");
    // Then
    assertHasAcl("/parent", "ip", "127.0.0.1", READ);
    assertHasAcl("/parent/a", "ip", "127.0.0.1", READ);
    assertHasAcl("/parent/a/b", "ip", "127.0.0.1", READ);
    assertHasAcl("/parent/a/b/c", "ip", "127.0.0.1", READ);
  }

  @Test
  public void testSupportsWildcard() throws Exception {
    // Given
    path("/abc123");
    path("/abcdef/efg");
    path("/abc/123");
    path("/x");
    path("/y/a");
    path("/ab");
    // When
    setAcls("/abc*", "ip:127.0.0.1:r");
    // Then
    assertHasAcl("/abc123", "ip", "127.0.0.1", READ);
    assertHasAcl("/abcdef/efg", "ip", "127.0.0.1", READ);
    assertHasAcl("/abc/123", "ip", "127.0.0.1", READ);
    assertHasAcl("/x", "world", "anyone", ALL);
    assertHasAcl("/y/a", "world", "anyone", ALL);
    assertHasAcl("/ab", "world", "anyone", ALL);
  }

  @Test
  public void testSupportsMultupleWildcards() throws Exception {
    // Given
    path("/abc123");
    path("/a/abcdef");
    path("/def/abc");
    path("/xy/abc/efg");
    path("/a/xyabc");
    path("/a/b/abc");
    path("/b");
    // When
    setAcls("/*/abc*", "ip:127.0.0.1:r");
    // Then
    assertHasAcl("/a/abcdef", "ip", "127.0.0.1", READ);
    assertHasAcl("/xy/abc/efg", "ip", "127.0.0.1", READ);
    assertHasAcl("/def/abc", "ip", "127.0.0.1", READ);
    assertHasAcl("/a/xyabc", "world", "anyone", ALL);
    assertHasAcl("/abc123", "world", "anyone", ALL);
    assertHasAcl("/a/b/abc", "world", "anyone", ALL);
    assertHasAcl("/b", "world", "anyone", ALL);
  }

  @Test
  public void testSupportsWorldScheme() throws Exception {
    // Given
    path("/unprotected");
    // When
    setAcls("/unprotected", "world:anyone:r");
    // Then
    assertHasAcl("/unprotected", "world", "anyone", READ);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectsUnsupportedScheme() throws Exception {
    path("/any");
    setAcls("/any", "unsupported:anyone:r");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRejectUnsupportedPermission() throws Exception {
    path("/any");
    setAcls("/any", "world:anyone:invalid");
  }

  @Test
  public void testIgnoresNonExistentNode() throws Exception {
    setAcls("/nonexistent", "world:anyone:rw");
  }

  @Before
  public void startZookeeper() throws Exception {
    zkTestServer = new TestingServer(Port.free());
    zkTestServer.start();
    cli = CuratorFrameworkFactory.newClient(zkTestServer.getConnectString(), new RetryOneTime(2000));
    cli.start();
  }

  @After
  public void stopZookeeper() throws IOException {
    cli.close();
    zkTestServer.stop();
  }

  private String path(String s) throws Exception {
    return cli.create().creatingParentsIfNeeded().forPath(s, "any".getBytes());
  }

  private void setAcls(String path, String acl) throws Exception {
    ZkMigrator.main(new String[] {
      "-connection-string", zkTestServer.getConnectString(),
      "-znode", path,
      "-acl", acl
    });
  }

  private void deleteZnode(String path) throws Exception {
    ZkMigrator.main(new String[] {
      "-connection-string", zkTestServer.getConnectString(),
      "-znode", path,
      "-delete"
    });
  }

  private void assertHasAcl(String path, String scheme, String id, int permission) throws Exception {
    List<ACL> acls = cli.getACL().forPath(path);
    assertEquals("expected 1 acl on " + path, 1, acls.size());
    assertEquals("acl on " + path, new Id(scheme, id), acls.get(0).getId());
    assertEquals(permission, acls.get(0).getPerms());
  }

  private void assertRemoved(String path) throws Exception {
    try {
      cli.getACL().forPath(path);
      assertTrue(false);
    } catch (KeeperException.NoNodeException e) {
      //expected
    }
  }
  private void assertHasNode(String path) throws Exception {
    try {
      cli.getACL().forPath(path);
    } catch (KeeperException.NoNodeException e) {
      assertTrue(false);
    }
  }

  static class Port {
    public static int free() throws IOException {
      ServerSocket socket = null;
      try {
        socket = new ServerSocket(0);
        return socket.getLocalPort();
      } finally {
        if (socket != null) {
          socket.close();
        }
      }
    }
  }
}
