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

package org.apache.ambari.view.pig;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.ViewResourceHandler;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public abstract class BasePigTest {
  protected ViewResourceHandler handler;
  protected ViewContext context;
  protected static File pigStorageFile;
  protected static File baseDir;
  protected Map<String, String> properties;

  protected static String DATA_DIRECTORY = "./target/PigTest";

  @BeforeClass
  public static void startUp() throws Exception {
    File baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    FileUtil.fullyDelete(baseDir);
  }

  @AfterClass
  public static void shutDown() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    handler = createNiceMock(ViewResourceHandler.class);
    context = createNiceMock(ViewContext.class);

    properties = new HashMap<String, String>();
    baseDir = new File(DATA_DIRECTORY)
        .getAbsoluteFile();
    pigStorageFile = new File("./target/PigTest/storage.dat")
        .getAbsoluteFile();

    properties.put("dataworker.storagePath", pigStorageFile.toString());
    properties.put("webhdfs.url", "webhdfs://host:1234");
    properties.put("webhcat.hostname", "localhost/templeton/v1");
    properties.put("webhcat.port", "50111");
    properties.put("webhcat.username", "admin");
    properties.put("scripts.dir", "/tmp/.pigscripts");
    properties.put("jobs.dir", "/tmp/.pigjobs");

    setupProperties(properties, baseDir);

    expect(context.getProperties()).andReturn(properties).anyTimes();
    expect(context.getUsername()).andReturn("ambari-qa").anyTimes();
    expect(context.getInstanceName()).andReturn("MyPig").anyTimes();

    replay(handler, context);
  }

  protected void setupProperties(Map<String, String> properties, File baseDir) throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  protected static <T> T getService(Class<T> clazz,
                                    final ViewResourceHandler viewResourceHandler,
                                    final ViewContext viewInstanceContext) {
    Injector viewInstanceInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ViewResourceHandler.class).toInstance(viewResourceHandler);
        bind(ViewContext.class).toInstance(viewInstanceContext);
      }
    });
    return viewInstanceInjector.getInstance(clazz);
  }
}
