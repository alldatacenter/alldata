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
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.ambari.view.pig.HDFSTest;
import org.apache.ambari.view.pig.persistence.DataStoreStorage;
import org.apache.ambari.view.pig.persistence.Storage;
import org.apache.ambari.view.pig.resources.scripts.ScriptService;
import org.apache.ambari.view.pig.persistence.utils.StorageUtil;
import org.apache.ambari.view.pig.resources.scripts.models.PigScript;
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.utils.UserLocal;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class ScriptTestHDFSUnmanaged extends HDFSTest {
  private ScriptService scriptService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void startUp() throws Exception {
    HDFSTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    HDFSTest.shutDown(); // super
    UserLocal.dropAllConnections(HdfsApi.class);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    handler = createNiceMock(ViewResourceHandler.class);
    context = createNiceMock(ViewContext.class);
    UserLocal.dropAllConnections(HdfsApi.class);
    StorageUtil.dropAllConnections();
  }

  @Test
  public void createScriptAutoCreateNoScriptsPath() throws IOException, InterruptedException {
    Map<String, String> properties = new HashMap<String, String>();
    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    pigStorageFile = new File("./target/BasePigTest/storage.dat")
        .getAbsoluteFile();

    properties.put("dataworker.storagePath", pigStorageFile.toString());
//        properties.put("scripts.dir", "/tmp/.pigscripts");
    properties.put("webhdfs.url", hdfsURI);

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();

    replay(handler, context);
    scriptService = getService(ScriptService.class, handler, context);

    thrown.expect(MisconfigurationFormattedException.class);
    doCreateScript("Test", null);
  }

  @Test
  public void createScriptAutoCreateUsername() throws IOException, InterruptedException {
    Map<String, String> properties = new HashMap<String, String>();
    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    pigStorageFile = new File("./target/BasePigTest/storage.dat")
        .getAbsoluteFile();

    properties.put("dataworker.storagePath", pigStorageFile.toString());
    properties.put("scripts.dir", "/tmp/.pigscripts");
    properties.put("webhdfs.url", hdfsURI);

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();

    replay(handler, context);
    scriptService = getService(ScriptService.class, handler, context);

    Response createdScript = doCreateScript("Test", null);
    String createdScriptPath = ((PigScript) ((JSONObject) createdScript.getEntity()).get("script")).getPigScript();
    Assert.assertTrue(createdScriptPath.startsWith("/tmp/.pigscripts/"));

    properties.put("dataworker.username", "luke");
    Response createdScript2 = doCreateScript("Test", null);
    String createdScriptPath2 = ((PigScript) ((JSONObject) createdScript2.getEntity()).get("script")).getPigScript();
    Assert.assertTrue(createdScriptPath2.startsWith("/tmp/.pigscripts/"));
  }

  @Test
  public void createScriptAutoCreateNoStoragePath() throws IOException, InterruptedException {
    Map<String, String> properties = new HashMap<String, String>();
    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    pigStorageFile = new File("./target/BasePigTest/storage.dat")
        .getAbsoluteFile();

//        properties.put("dataworker.storagePath", pigStorageFile.toString());
    properties.put("scripts.dir", "/tmp/.pigscripts");
    properties.put("webhdfs.url", hdfsURI);

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();

    replay(handler, context);

    Storage storage = StorageUtil.getInstance(context).getStorage();
    Assert.assertEquals(DataStoreStorage.class.getSimpleName(), storage.getClass().getSimpleName());
  }

  @Test
  public void hdfsApiNoUsernameProvided() throws IOException, InterruptedException {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("webhdfs.url", hdfsURI);

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();

    replay(context);

    // no webhdfs.username property
    Assert.assertEquals("ambari-qa", HdfsUtil.getHdfsUsername(context));

    // with webhdfs.username property
    properties.put("webhdfs.username", "luke");
    Assert.assertEquals("luke", HdfsUtil.getHdfsUsername(context));
  }

  private Response doCreateScript(String title, String path) {
    return ScriptTest.doCreateScript(title, path, scriptService);
  }
}
