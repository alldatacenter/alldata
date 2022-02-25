/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.configuration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.fail;

import java.io.File;

import org.awaitility.core.ConditionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SingleFileWatchTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();
  private SingleFileWatch watchDog;
  private File fileToWatch;
  private volatile int numberOfEventsReceived = 0;

  @Before
  public void setUp() throws Exception {
    tmp.create();
    fileToWatch = tmp.newFile();
    watchDog = new SingleFileWatch(fileToWatch, file -> numberOfEventsReceived++);
    watchDog.start();
  }

  @After
  public void tearDown() throws Exception {
    watchDog.stop();
  }

  @Test
  public void testTriggersEventsOnMultipleFileChange() throws Exception {
    changeFile("change1");
    hasReceivedChangeEvents(1);
    changeFile("change2");
    hasReceivedChangeEvents(2);
    changeFile("change3");
    hasReceivedChangeEvents(3);
  }

  private void hasReceivedChangeEvents(int expectedNumberOfEvents) {
    try {
      await().atMost(8, SECONDS).until(() -> numberOfEventsReceived == expectedNumberOfEvents);
    } catch (ConditionTimeoutException e) {
      fail("Expected number of file change events: " + expectedNumberOfEvents + " but received: " + numberOfEventsReceived);
    }
  }

  private void changeFile(String content) throws Exception {
    long lastModified = fileToWatch.lastModified();
    while (lastModified == fileToWatch.lastModified()) {
      writeStringToFile(fileToWatch, content, "UTF-8");
    }
  }
}