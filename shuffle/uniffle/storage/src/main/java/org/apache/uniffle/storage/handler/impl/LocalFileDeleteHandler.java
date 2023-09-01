/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.storage.handler.impl;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.storage.handler.api.ShuffleDeleteHandler;

public class LocalFileDeleteHandler implements ShuffleDeleteHandler {

  private static final Logger LOG = LoggerFactory.getLogger(LocalFileDeleteHandler.class);

  @Override
  public void delete(String[] shuffleDataStoredPath, String appId, String user) {
    for (String basePath : shuffleDataStoredPath) {
      final String shufflePath = basePath;
      long start = System.currentTimeMillis();
      try {
        File baseFolder = new File(shufflePath);
        FileUtils.deleteDirectory(baseFolder);
        LOG.info("Delete shuffle data for appId[" + appId + "] with " + shufflePath
            + " cost " + (System.currentTimeMillis() - start) + " ms");
      } catch (Exception e) {
        LOG.warn("Can't delete shuffle data for appId[" + appId + "] with " + shufflePath, e);
      }
    }
  }
}
