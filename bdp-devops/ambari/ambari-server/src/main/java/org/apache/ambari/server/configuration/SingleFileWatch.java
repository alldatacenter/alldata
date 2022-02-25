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

import java.io.File;
import java.util.function.Consumer;

import org.apache.log4j.helpers.FileWatchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watchdog that notifies a listener on a file content change.
 */
public class SingleFileWatch {
  private static final Logger LOG = LoggerFactory.getLogger(SingleFileWatch.class);
  private final File file;
  private final FileWatchdog watchdog;
  private final Consumer<File> changeListener;
  private volatile boolean started = false;

  /**
   * @param file to be watched
   * @param changeListener to be notified if the file content changes
   */
  public SingleFileWatch(File file, Consumer<File> changeListener) {
    this.file = file;
    this.changeListener = changeListener;
    this.watchdog = createWatchDog();
  }

  private FileWatchdog createWatchDog() {
    FileWatchdog fileWatch = new FileWatchdog(file.getAbsolutePath()) {
      @Override
      protected void doOnChange() {
        if (started) {
          notifyListener();
        }
      }
    };
    fileWatch.setDelay(1000);
    fileWatch.setDaemon(true);
    fileWatch.setName("FileWatchdog:" + file.getName());
    return fileWatch;
  }

  private void notifyListener() {
    LOG.info("{} changed. Sending notification.", file);
    try {
      changeListener.accept(file);
    } catch (Exception e) {
      LOG.warn("Error while notifying " + this + " listener", e);
    }
  }

  /**
   * Start the watch service in the background
   */
  public void start() {
    LOG.info("Starting " + this);
    started = true;
    watchdog.start();
  }

  /**
   * Stop the watch service
   */
  public void stop() {
    LOG.info("Stopping " + this);
    started = false;
    watchdog.interrupt();
  }

  @Override
  public String toString() {
    return "SingleFileWatcher:" + file.getName();
  }
}
