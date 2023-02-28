///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.manage.servlet;
//
//import com.qlangtech.tis.coredefine.module.action.CollectionTopology;
//import com.qlangtech.tis.manage.common.AppDomainInfo;
//import com.qlangtech.tis.manage.common.RunContext;
//import com.qlangtech.tis.manage.servlet.QueryIndexServlet.SolrQueryModuleCreator;
//import org.apache.commons.lang.StringUtils;
////import org.apache.solr.common.cloud.DocCollection;
////import org.apache.solr.common.cloud.Replica;
////import org.apache.solr.common.cloud.Slice;
////import org.apache.solr.common.cloud.TISZkStateReader;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年12月23日 下午6:58:13
// */
//public class SolrCloudQueryResutStrategy extends QueryResutStrategy {
//
//  protected final RunContext runContext;
//
//  private static final String SHARD_PREIX = "shard";
//
//  SolrCloudQueryResutStrategy(AppDomainInfo domain, RunContext runContext, SolrQueryModuleCreator creator) {
//    super(domain, creator);
//    this.runContext = runContext;
//  }
//
//  // @Override
//  // public int getServicePort() {
//  // return 7001;
//  // }
//  @Override
//  public List<ServerJoinGroup> query() {
//    throw new UnsupportedOperationException();
////    List<ServerJoinGroup> result = new ArrayList<ServerJoinGroup>();
////    ServerJoinGroup groupServer = null;
////    TISZkStateReader clusterReader = runContext.getZkStateReader();
////    DocCollection docCollection = TISZkStateReader.getCollectionLive(clusterReader, domain.getAppName());
////    if (docCollection == null) {
////      throw new IllegalStateException("collection:" + domain.getAppName() + " relevant docCollection can not be null");
////    }
////    Map<String, Slice> groups = docCollection.getSlicesMap();
////    short shard;
////    for (Map.Entry<String, Slice> entry : groups.entrySet()) {
////
////      for (Replica replic : entry.getValue().getReplicas()) {
////        groupServer = new ServerJoinGroup();
////        groupServer.setLeader(replic.getBool("leader", false));
////        groupServer.setIpAddress(replic.getCoreUrl());
////        groupServer.setReplicBaseUrl(replic.getBaseUrl());
////        shard = (Short.parseShort(StringUtils.substringAfter(entry.getKey(), SHARD_PREIX)));
////        groupServer.setGroupIndex(--shard);
////        result.add(groupServer);
////      }
////    }
////    return result;
//  }
//
//  public CollectionTopology createCollectionTopology() {
//    throw new UnsupportedOperationException();
////    CollectionTopology topology = new CollectionTopology();
////    TISZkStateReader clusterReader = runContext.getZkStateReader();
////    DocCollection docCollection = TISZkStateReader.getCollectionLive(clusterReader, domain.getAppName());
////    if (docCollection == null) {
////      throw new IllegalStateException("collection:" + domain.getAppName() + " relevant docCollection can not be null");
////    }
////    CollectionTopology.Shared shared = null;
////    Map<String, Slice> groups = docCollection.getSlicesMap();
////    short shard;
////    for (Map.Entry<String, Slice> entry : groups.entrySet()) {
////      shared = new CollectionTopology.Shared(entry.getKey());
////      topology.addShard(shared);
////      // shardName = entry.getKey();
////      for (Replica replic : entry.getValue().getReplicas()) {
////        shared.addReplic(replic);
////        // groupServer = new ServerJoinGroup();
////        // groupServer.setLeader(replic.getBool("leader", false));
////        // groupServer.setIpAddress(replic.getCoreUrl());
////        // shard = (Short.parseShort(StringUtils.substringAfter(entry.getKey(), SHARD_PREIX)));
////        // groupServer.setGroupIndex(--shard);
////        // result.add(groupServer);
////      }
////    }
////    return topology;
//  }
////
////  public static QueryResutStrategy create(final AppDomainInfo domain, SolrQueryModuleCreator creator, RunContext runContext) {
////    // if (domain.isAutoDeploy()) {
////    return new SolrCloudQueryResutStrategy(domain, runContext, creator);
////    // } else {
////    // return new NormalQueryResutStrategy(domain, runContext, creator);
////    // }
////  }
//}
