///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.manage.spring;
//
////import com.qlangtech.tis.TisZkClient;
//
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import org.apache.solr.common.cloud.TISZkStateReader;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年10月13日 下午5:08:51
// */
//public class ClusterStateReader extends EnvironmentBindService<TISZkStateReader> {
//
//  private ZooKeeperGetter zooKeeperGetter;
//
//  private static final Logger LOG = LoggerFactory.getLogger(ClusterStateReader.class);
//
//  @Override
//  protected TISZkStateReader createSerivce(RunEnvironment runtime) {
////    try {
////      ITISCoordinator zkClinet =  zooKeeperGetter.getInstance(runtime);
////      final TISZkStateReader zkStateReader = new TISZkStateReader(zkClinet.getZK());
////      zkClinet.addOnReconnect(() -> {
////        try {
////          zkStateReader.createClusterStateWatchersAndUpdate();
////        } catch (KeeperException e) {
////          LOG.error("A ZK error has occurred", e);
////          throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "A ZK error has occurred", e);
////        } catch (InterruptedException e) {
////          // Restore the interrupted status
////          Thread.currentThread().interrupt();
////          LOG.error("Interrupted", e);
////          throw new ZooKeeperException(SolrException.ErrorCode.SERVER_ERROR, "Interrupted", e);
////        }
////      });
////      zkStateReader.createClusterStateWatchersAndUpdate();
////      return zkStateReader;
////    } catch (Exception e) {
////      throw new RuntimeException(e);
////    }
//    throw new UnsupportedOperationException();
//  }
//
//  public void setZooKeeperGetter(ZooKeeperGetter zooKeeperGetter) {
//    this.zooKeeperGetter = zooKeeperGetter;
//  }
//}
