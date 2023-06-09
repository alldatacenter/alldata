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
package com.qlangtech.tis;

import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.manage.common.*;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.manage.spring.EnvironmentBindService;
import com.qlangtech.tis.test.TISEasyMock;
import org.apache.commons.io.IOUtils;
import org.apache.struts2.StrutsSpringTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-06 18:04
 */
public class BasicActionTestCase extends StrutsSpringTestCase implements TISEasyMock {

  //private static List<Object> mocks = Lists.newArrayList();
  protected RunContext runContext;


  protected void setCollection(String collection) {
    request.addHeader("appname", collection);
    DefaultFilter.AppAndRuntime app = new DefaultFilter.AppAndRuntime();
    app.setAppName(collection);
    DefaultFilter.setAppAndRuntime(app);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();
    CenterResource.setNotFetchFromCenterRepository();
    HttpUtils.addMockGlobalParametersConfig();
    System.out.println("==============execute setUp");
    this.clearMocks();
    EnvironmentBindService.cleanCacheService();
    this.runContext = Objects.requireNonNull(applicationContext.getBean("runContextGetter", RunContextGetter.class)
      , "runContextGetter can not be null").get();
  }

  protected JSONObject getJSON(String filename) {
    try {
      try (InputStream input = this.getClass().getResourceAsStream(filename)) {

        return JSONObject.parseObject(IOUtils.toString(input, TisUTF8.get()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  protected AjaxValve.ActionExecResult showBizResult() {
    AjaxValve.ActionExecResult actionExecResult = MockContext.getActionExecResult();
    if (!actionExecResult.isSuccess()) {
      System.err.println(AjaxValve.buildResultStruct(MockContext.instance));
      // actionExecResult.getErrorPageShow()
    } else {
      System.out.println(AjaxValve.buildResultStruct(MockContext.instance));
    }
    return actionExecResult;
  }

//  protected DocCollection createMockCollection(String collection) throws Exception {
//    return this.createMockCollection(collection, true);
//  }

//  protected DocCollection createMockCollection(String collection, boolean fetchCollectionState) throws Exception {
//    TISZkStateReader tisZkStateReader = this.mock("tisZkStateReader", TISZkStateReader.class);
//    //getClusterState
//    ClusterState clusterState = this.mock("clusterState", ClusterState.class);
//    EasyMock.expect(tisZkStateReader.getClusterState()).andReturn(clusterState).anyTimes();
//
//    ClusterState.CollectionRef collectionRef
//      = this.mock(collection + "CollectionRef", ClusterState.CollectionRef.class);
//    EasyMock.expect(clusterState.getCollectionRef(collection)).andReturn(collectionRef).anyTimes();
//    DocCollection coll = null;
//    coll = buildDocCollectionMock(false, fetchCollectionState, collection, tisZkStateReader);
//    EasyMock.expect(collectionRef.get()).andReturn(coll).anyTimes();
//
//    MockClusterStateReader.mockStateReader = tisZkStateReader;
//    return coll;
//  }

  protected static final String replica_core_url = "http://192.168.28.200:8080/solr/search4employees_shard1_replica_n1/";
  public static final String name_shard1 = "shard1";

//  protected DocCollection buildDocCollectionMock(boolean getSlicesMap, boolean fetchCollectionState, String collectionName, TISZkStateReader tisZkStateReader) throws Exception {
//    DocCollection docCollection = this.mock("docCollection", DocCollection.class);
//    if (fetchCollectionState) {
//      Map<String, Slice> sliceMap = Maps.newHashMap();
//      Slice slice = this.mock("shard1Slice", Slice.class);
//      Replica replica = this.mock("core_node2_replica", Replica.class);
//      sliceMap.put(name_shard1, slice);
//      IExpectationSetters<Collection<Replica>> collectionIExpectationSetters
//        = EasyMock.expect(slice.getReplicas()).andReturn(Collections.singleton(replica));
//      collectionIExpectationSetters.anyTimes();
////      if (!getSlicesMap) {
////        collectionIExpectationSetters.times(2);
////      }
//      IExpectationSetters<Boolean> leaderSetters
//        = EasyMock.expect(replica.getBool("leader", false)).andReturn(true);
//      leaderSetters.anyTimes();
////      if (!getSlicesMap) {
////        leaderSetters.times(1);
////      }
//      IExpectationSetters<String> getCoreUrlExpectationSetters
//        = EasyMock.expect(replica.getCoreUrl()).andReturn(replica_core_url);
//      getCoreUrlExpectationSetters.anyTimes();
//      if (!getSlicesMap) {
//        String shard1 = name_shard1;
//
//        EasyMock.expect(slice.getName()).andReturn(name_shard1).anyTimes();
//
//        EasyMock.expect(replica.getName()).andReturn(collectionName + "_shard1_replica_n1").anyTimes();
//        EasyMock.expect(replica.getBaseUrl()).andReturn("baseurl").anyTimes();
//        EasyMock.expect(replica.getCollection()).andReturn(collectionName).anyTimes();
//        EasyMock.expect(replica.getCoreName()).andReturn(collectionName + "_shard1_replica_n1").anyTimes();
//        EasyMock.expect(replica.getState()).andReturn(Replica.State.ACTIVE).anyTimes();
//        EasyMock.expect(replica.getNodeName()).andReturn("192.168.28.200:8080_solr").anyTimes();
//        EasyMock.expect(replica.getProperties()).andReturn(Collections.emptyMap()).anyTimes();
//        EasyMock.expect(replica.getType()).andReturn(Replica.Type.NRT).anyTimes();
//        EasyMock.expect(replica.getSlice()).andReturn(shard1).anyTimes();
//
//        EasyMock.expect(docCollection.getName()).andReturn(collectionName).anyTimes();
//        EasyMock.expect(docCollection.getSlices()).andReturn(sliceMap.values()).anyTimes();
//      } else {
//        //  getCoreUrlExpectationSetters.times(1);
//        EasyMock.expect(replica.getBaseUrl()).andReturn("baseurl").anyTimes();
//      }
//
//      EasyMock.expect(docCollection.getSlicesMap()).andReturn(sliceMap).anyTimes();
//
//
//      EasyMock.expect(tisZkStateReader.fetchCollectionState(collectionName, null))
//        .andReturn(docCollection).anyTimes();
//    }
//    return docCollection;
//  }

//  protected IExpectationSetters<byte[]> createCoordinatorMock(Consumer<ITISCoordinator> consumer) throws IOException {
//    return createCoordinatorMock(true, consumer);
//  }
//
//  protected IExpectationSetters<byte[]> createCoordinatorMock(boolean overseer_elect_leader, Consumer<ITISCoordinator> consumer) throws IOException {
//    ITISCoordinator zkCoordinator =  MockZKUtils.createZkMock();
//    MockZooKeeperGetter.mockCoordinator = zkCoordinator;
//    consumer.accept(zkCoordinator);
//    if (overseer_elect_leader) {
//      try (InputStream input = this.getClass().getResourceAsStream("/com/qlangtech/tis/overseer_elect_leader.json")) {
//        IExpectationSetters<byte[]> expect = EasyMock.expect(
//          zkCoordinator.getData(ZkUtils.ZK_PATH_OVERSEER_ELECT_LEADER, null, new Stat(), true));
//        expect.andReturn(IOUtils.toByteArray(input));
//        return expect;
//      }
//    }
//    return null;
//  }

  @Override
  protected String[] getContextLocations() {
    return new String[]{"classpath:/tis.application.context.xml", "classpath:/tis.test.context.xml"};
  }
}
