/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
// */
package com.qlangtech.tis.coredefine.module.control;

import com.qlangtech.tis.runtime.module.action.BasicModule;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public class SelectableServer {

  static final String key = SelectableServer.class.getName() + ".candidateServer";

//  private static Collection<CoreNode> getSelectableNodes(TISZkStateReader stateReader) throws KeeperException, InterruptedException {
//    return stateReader.getSelectTableNodes();
//  }


  @SuppressWarnings("all")
  public static CoreNode[] getCoreNodeInfo(HttpServletRequest request, BasicModule module
    , boolean excludeHaveAppServers, boolean isAppNameAware) {
//    try {
//      CoreNode[] result = null;
//      if ((result = (CoreNode[]) request.getAttribute(key)) == null) {
//        Collection<CoreNode> nodes = getSelectableNodes(module.getZkStateReader());
//        result = nodes.toArray(new CoreNode[0]);
//        request.setAttribute(key, result);
//        if (excludeHaveAppServers) {
//          // 过滤掉那些已经部署了应用的服务器
//          List<CoreNode> excludeAppServers = new ArrayList<CoreNode>();
//          for (CoreNode node : result) {
//            if (node.getSolrCoreCount() < 1) {
//              excludeAppServers.add(node);
//            }
//          }
//          result = excludeAppServers.toArray(new CoreNode[excludeAppServers.size()]);
//          request.setAttribute(key, result);
//        }
//      }
//      return result;
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
    throw new UnsupportedOperationException();
  }

  //
  public static class ServerNodeTopology {

    private int shardCount;

    private int replicaCount;

    private CoreNode[] hosts;

    public int getShardCount() {
      return shardCount;
    }

    public void setShardCount(int shardCount) {
      this.shardCount = shardCount;
    }

    public int getReplicaCount() {
      return replicaCount;
    }

    public void setReplicaCount(int replicaCount) {
      this.replicaCount = replicaCount;
    }

    public CoreNode[] getHosts() {
      return hosts;
    }

    public void setHosts(CoreNode[] hosts) {
      this.hosts = hosts;
    }
  }

  //
  public static class CoreNode {

    private String nodeName;

    private String luceneSpecVersion;

    private String hostName;

    private int solrCoreCount;

    public String getNodeName() {
      return this.nodeName;
    }

    public String getHostName() {
      return this.hostName;
    }

    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    public String getLuceneVersion() {
      return luceneSpecVersion;
    }

    public String getLuceneSpecVersion() {
      return luceneSpecVersion;
    }

    public void setLuceneSpecVersion(String luceneSpecVersion) {
      this.luceneSpecVersion = luceneSpecVersion;
    }

    public void setNodeName(String nodeName) {
      this.nodeName = nodeName;
    }

    public int getSolrCoreCount() {
      return solrCoreCount;
    }

    public void setSolrCoreCount(int solrCoreCount) {
      this.solrCoreCount = solrCoreCount;
    }
  }
}
