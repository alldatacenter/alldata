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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.zk;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;

/**
 * Register this App Master in ZK to prevent duplicates.
 * <p>
 * Possible enhancement is to put the registry in some well-known location, such
 * as /drill-am,
 */
public class AMRegistry {
  private static final String AM_REGISTRY = "/drill-on-yarn";

  private ZKClusterCoordinator zkCoord;
  @SuppressWarnings("unused")
  private String amHost;
  @SuppressWarnings("unused")
  private int amPort;
  @SuppressWarnings("unused")
  private String amAppId;

  private String zkRoot;

  private String clusterId;

  public AMRegistry(ZKClusterCoordinator zkCoord) {
    this.zkCoord = zkCoord;
  }

  public void useLocalRegistry(String zkRoot, String clusterId) {
    this.zkRoot = zkRoot;
    this.clusterId = clusterId;
  }

  /**
   * Register this AM as an ephemeral znode in ZK. The structure of ZK is as
   * follows:
   *
   * <pre>
   * /drill
   * . &lt;cluster-id>
   * . . &lt;Drillbit GUID> (Value is Proto-encoded drillbit info)
   * . drill-on-yarn
   * . . &lt;cluster-id> (value: amHost:port)
   * </pre>
   * <p>
   * The structure acknowledges that the cluster-id znode may be renamed, and
   * there may be multiple cluster IDs for a single drill root node. (Odd, but
   * supported.) To address this, we put the AM registrations in their own
   * (persistent) znode: drill-on-yarn. Each is keyed by the cluster ID (so we
   * can find it), and holds the host name, HTTP port and Application ID of the
   * AM.
   * <p>
   * When the AM starts, it atomically checks and sets the AM registration. If
   * another AM already is running, then this AM will fail, displaying a log
   * error message with the host, port and (most importantly) app ID so the user
   * can locate the problem.
   *
   * @throws ZKRuntimeException
   */

  public void register(String amHost, int amPort, String amAppId)
      throws ZKRuntimeException {
    this.amHost = amHost;
    this.amPort = amPort;
    this.amAppId = amAppId;
    try {

      // The znode to hold AMs may or may not exist. Create it if missing.

      try {
        zkCoord.getCurator().create().withMode(CreateMode.PERSISTENT)
            .forPath(AM_REGISTRY, new byte[0]);
      } catch (NodeExistsException e) {
        // OK
      }

      // Try to create the AM registration.

      String amPath = AM_REGISTRY + "/" + clusterId;
      String content = amHost + ":" + Integer.toString(amPort) + ":" + amAppId;
      try {
        zkCoord.getCurator().create().withMode(CreateMode.EPHEMERAL)
            .forPath(amPath, content.getBytes("UTF-8"));
      } catch (NodeExistsException e) {

        // ZK says that a node exists, which means that another AM is already
        // running.
        // Display an error, handling the case where the AM just disappeared,
        // the
        // registration is badly formatted, etc.

        byte data[] = zkCoord.getCurator().getData().forPath(amPath);
        String existing;
        if (data == null) {
          existing = "Unknown";
        } else {
          String packed = new String(data, "UTF-8");
          String unpacked[] = packed.split(":");
          if (unpacked.length < 3) {
            existing = packed;
          } else {
            existing = unpacked[0] + ", port: " + unpacked[1]
                + ", Application ID: " + unpacked[2];
          }
        }

        // Die with a clear (we hope!) error message.

        throw new ZKRuntimeException(
            "FAILED! An Application Master already exists for " + zkRoot + "/"
                + clusterId + " on host: " + existing);
      }
    } catch (ZKRuntimeException e) {

      // Something bad happened with ZK.

      throw e;
    } catch (Exception e) {

      // Something bad happened with ZK.

      throw new ZKRuntimeException("Failed to create AM registration node", e);
    }
  }

}
