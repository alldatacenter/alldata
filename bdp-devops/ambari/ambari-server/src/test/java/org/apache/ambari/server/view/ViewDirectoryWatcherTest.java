/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.view;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.io.FileDeleteStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Function;

public class ViewDirectoryWatcherTest {

  private static final Configuration configuration = createNiceMock(Configuration.class);
  private static final ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
  private File testDir;


  @Before
  public void setUp() throws Exception {
    reset(configuration, viewRegistry);
    testDir = new File(System.getProperty("java.io.tmpdir"), "test_dir");
    if (testDir.exists()) {
      FileDeleteStrategy.FORCE.delete(testDir);
    }
    testDir.mkdirs();

  }


  @Test
  public void testDirectoryWatcherStart() throws Exception {
    ViewDirectoryWatcher viewDirectoryWatcher = new ViewDirectoryWatcher();

    expect(configuration.getViewsDir()).andReturn(testDir).once();
    viewDirectoryWatcher.configuration = configuration;
    viewDirectoryWatcher.viewRegistry = viewRegistry;
    replay(configuration);
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    viewDirectoryWatcher.addHook(new Function<Path, Boolean>() {
      @Nullable
      @Override
      public Boolean apply(@Nullable Path path) {
        countDownLatch.countDown();
        return true;
      }

    });
    viewDirectoryWatcher.start();
    countDownLatch.await(1, SECONDS);
    // Expect watecher to start
    Assert.assertTrue(viewDirectoryWatcher.isRunning());
    verify(configuration);
  }


  @Test
  public void testDirectoryExtractionOnFileAdd() throws Exception {
    ViewDirectoryWatcher viewDirectoryWatcher = new ViewDirectoryWatcher();
    expect(configuration.getViewsDir()).andReturn(testDir).once();
    viewDirectoryWatcher.configuration = configuration;
    viewDirectoryWatcher.viewRegistry = viewRegistry;
    viewRegistry.readViewArchive(Paths.get(testDir.getAbsolutePath(), "file.jar"));
    replay(configuration, viewRegistry);
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    viewDirectoryWatcher.addHook(new Function<Path, Boolean>() {
      @Nullable
      @Override
      public Boolean apply(@Nullable Path path) {
        countDownLatch.countDown();
        return true;
      }
    });
    viewDirectoryWatcher.start();
    // Create a new File at destination
    createZipFile();
    countDownLatch.await(30, SECONDS);

    // Expect watcher to respond
    verify(configuration, viewRegistry);
  }


  @Test
  public void testDirectoryWatcherStop() throws Exception {

    ViewDirectoryWatcher viewDirectoryWatcher = new ViewDirectoryWatcher();
    expect(configuration.getViewsDir()).andReturn(testDir).once();
    viewDirectoryWatcher.configuration = configuration;
    viewDirectoryWatcher.viewRegistry = viewRegistry;
    replay(configuration);

    viewDirectoryWatcher.start();
    //Time to start
    Thread.sleep(100);
    viewDirectoryWatcher.stop();
    Assert.assertFalse(viewDirectoryWatcher.isRunning());
    verify(configuration);
  }


  private void createZipFile() throws IOException {
    File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "view.xml");
    file.createNewFile();

    // input file
    FileInputStream in = new FileInputStream(file);

    // out put file
    ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(testDir, "file.jar")));

    // name the file inside the zip  file
    out.putNextEntry(new ZipEntry("view.xml"));

    // buffer size
    byte[] b = new byte[1024];
    int count;

    while ((count = in.read(b)) > 0) {
      System.out.println();
      out.write(b, 0, count);
    }
    out.close();
    in.close();

  }

}




