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

import static java.util.Collections.singletonList;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;

/**
 * ZkAcl represent a ZooKeeper ACL (scheme, id and permissions)
 */
public class ZkAcl {
  private static final int ANY_NODE_VER = -1;
  private final AclScheme scheme;
  private final String id;
  private final Permission permission;

  /**
   * Creates an instance of me by parsing the given string
   * @param acl in the following format scheme:id:permissions e.g.: world:anyone:crdw
     */
  public static ZkAcl parse(String acl) {
    String[] parts = acl.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid ACL: " + acl + " must be <scheme:id:permission>");
    }
    return new ZkAcl(AclScheme.parse(parts[0]), parts[1], Permission.parse(parts[2]));
  }

  private ZkAcl(AclScheme scheme, String id, Permission permission) {
    this.scheme = scheme;
    this.id = id;
    this.permission = permission;
  }

  /**
   * Sets the ACL on the znode indicated by the given pattern
   */
  public void setRecursivelyOn(ZooKeeper zkClient, ZkPathPattern pattern) throws KeeperException, InterruptedException {
    for (String each : pattern.findMatchingPaths(zkClient, "/")) {
      System.out.println("Set ACL " + asZkAcl() + " recursively on " + each);
      setRecursivelyOnSingle(zkClient, each);
    }
  }

  public void setRecursivelyOnSingle(ZooKeeper zkClient, String baseNode) throws KeeperException, InterruptedException {
    zkClient.setACL(baseNode, singletonList(asZkAcl()), ANY_NODE_VER);
    for (String child : zkClient.getChildren(baseNode, null)) {
      setRecursivelyOnSingle(zkClient, append(baseNode, child));
    }
  }

  private ACL asZkAcl() {
    return new ACL(permission.code, new Id(scheme.value, id));
  }

  public static String append(String node, String child) {
    return node.endsWith("/") ? node + child : node + "/" + child;
  }

  static class AclScheme {
    final String value;

    public static AclScheme parse(String scheme) {
      if (scheme.toLowerCase().equals("world") || scheme.toLowerCase().equals("ip")) {
        return new AclScheme(scheme);
      }
      throw new IllegalArgumentException("Unsupported scheme: " + scheme);
    }

    private AclScheme(String value) {
      this.value = value;
    }
  }

  static class Permission {
    final int code;

    public static Permission parse(String permission) {
      int permissionCode = 0;
      for (char each : permission.toLowerCase().toCharArray()) {
        switch (each) {
          case 'r': permissionCode |= ZooDefs.Perms.READ; break;
          case 'w': permissionCode |= ZooDefs.Perms.WRITE; break;
          case 'c': permissionCode |= ZooDefs.Perms.CREATE; break;
          case 'd': permissionCode |= ZooDefs.Perms.DELETE; break;
          case 'a': permissionCode |= ZooDefs.Perms.ADMIN; break;
          default: throw new IllegalArgumentException("Unsupported permission: " + permission);
        }
      }
      return new Permission(permissionCode);
    }

    private Permission(int code) {
      this.code = code;
    }
  }
}
