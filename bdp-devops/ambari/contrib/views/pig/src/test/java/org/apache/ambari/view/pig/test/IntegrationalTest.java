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

package org.apache.ambari.view.pig.test;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.HDFSTest;
import org.apache.ambari.view.pig.persistence.Storage;
import org.apache.ambari.view.pig.persistence.utils.StorageUtil;
import org.apache.ambari.view.pig.resources.jobs.JobService;
import org.apache.ambari.view.pig.resources.scripts.ScriptService;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.utils.UserLocalObjects;
import org.apache.ambari.view.utils.UserLocal;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.junit.*;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.replay;

/**
 * Test cases that verify all services altogether
 */
public class IntegrationalTest extends HDFSTest {
  private JobService jobService;
  private ScriptService scriptService;

  @BeforeClass
  public static void startUp() throws Exception {
    HDFSTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    HDFSTest.shutDown(); // super
    UserLocal.dropAllConnections(HdfsApi.class); //cleanup API connection
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    jobService = getService(JobService.class, handler, context);
    scriptService = getService(ScriptService.class, handler, context);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    UserLocal.dropAllConnections(TempletonApi.class);
    UserLocal.dropAllConnections(HdfsApi.class);
  }

  @Test
  public void testHdfsApiDependsOnInstance() throws Exception {
    UserLocal.dropAllConnections(HdfsApi.class); //cleanup API connection

    ViewContext context1 = createNiceMock(ViewContext.class);
    ViewContext context2 = createNiceMock(ViewContext.class);
    ViewContext context3 = createNiceMock(ViewContext.class);
    expect(context1.getProperties()).andReturn(properties).anyTimes();
    expect(context1.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context1.getInstanceName()).andReturn("Pig1").anyTimes();

    expect(context2.getProperties()).andReturn(properties).anyTimes();
    expect(context2.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context2.getInstanceName()).andReturn("Pig2").anyTimes();

    expect(context3.getProperties()).andReturn(properties).anyTimes();
    expect(context3.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context3.getInstanceName()).andReturn("Pig1").anyTimes();

    replay(context1, context2, context3);

    HdfsApi hdfsApi1 = UserLocalObjects.getHdfsApi(context1);
    HdfsApi hdfsApi2 = UserLocalObjects.getHdfsApi(context2);
    Assert.assertNotSame(hdfsApi1, hdfsApi2);

    HdfsApi hdfsApi1_2 = UserLocalObjects.getHdfsApi(context1);
    HdfsApi hdfsApi2_2 = UserLocalObjects.getHdfsApi(context1);
    Assert.assertSame(hdfsApi1_2, hdfsApi2_2);

    HdfsApi hdfsApi1_3 = UserLocalObjects.getHdfsApi(context1);
    HdfsApi hdfsApi3_3 = UserLocalObjects.getHdfsApi(context3);
    Assert.assertSame(hdfsApi1_3, hdfsApi3_3);
  }

  @Test
  public void testStorageDependsOnInstance() throws Exception {
    StorageUtil.dropAllConnections();

    ViewContext context1 = createNiceMock(ViewContext.class);
    ViewContext context2 = createNiceMock(ViewContext.class);
    ViewContext context3 = createNiceMock(ViewContext.class);
    expect(context1.getProperties()).andReturn(properties).anyTimes();
    expect(context1.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context1.getInstanceName()).andReturn("Pig1").anyTimes();

    expect(context2.getProperties()).andReturn(properties).anyTimes();
    expect(context2.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context2.getInstanceName()).andReturn("Pig2").anyTimes();

    expect(context3.getProperties()).andReturn(properties).anyTimes();
    expect(context3.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context3.getInstanceName()).andReturn("Pig1").anyTimes();

    replay(context1, context2, context3);

    Storage storage1 = StorageUtil.getInstance(context1).getStorage();
    Storage storage2 = StorageUtil.getInstance(context2).getStorage();
    Assert.assertNotSame(storage1, storage2);

    Storage storage1_2 = StorageUtil.getInstance(context1).getStorage();
    Storage storage2_2 = StorageUtil.getInstance(context1).getStorage();
    Assert.assertSame(storage1_2, storage2_2);

    Storage storage1_3 = StorageUtil.getInstance(context1).getStorage();
    Storage storage3_3 = StorageUtil.getInstance(context3).getStorage();
    Assert.assertSame(storage1_3, storage3_3);
  }
}
