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

package org.apache.uniffle.coordinator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.filesystem.HadoopFilesystemProvider;
import org.apache.uniffle.common.util.ThreadUtils;

public class ClientConfManager implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ClientConfManager.class);

  private Map<String, String> clientConf = Maps.newConcurrentMap();
  private final AtomicLong lastCandidatesUpdateMS = new AtomicLong(0L);
  private Path path;
  private ScheduledExecutorService updateClientConfSES = null;
  private FileSystem fileSystem;
  private static final String WHITESPACE_REGEX = "\\s+";
  private ApplicationManager applicationManager;

  public ClientConfManager(CoordinatorConf conf, Configuration hadoopConf,
      ApplicationManager applicationManager) throws Exception {
    if (conf.getBoolean(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED)) {
      this.applicationManager = applicationManager;
      init(conf, hadoopConf);
    }
  }

  private void init(CoordinatorConf conf, Configuration hadoopConf) throws Exception {
    String pathStr = conf.get(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH);
    this.path = new Path(pathStr);

    this.fileSystem = HadoopFilesystemProvider.getFilesystem(path, hadoopConf);

    if (!fileSystem.isFile(path)) {
      String msg = String.format("Fail to init ClientConfManager, %s is not a file.", path.toUri());
      LOG.error(msg);
      throw new IllegalStateException(msg);
    }
    updateClientConfInternal();
    LOG.info("Load client conf from " + pathStr + " successfully");

    int updateIntervalS = conf.getInteger(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC);
    updateClientConfSES = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("ClientConfManager-%d"));
    updateClientConfSES.scheduleAtFixedRate(
        this::updateClientConf, 0, updateIntervalS, TimeUnit.SECONDS);
  }

  private void updateClientConf() {
    try {
      FileStatus[] fileStatus = fileSystem.listStatus(path);
      if (!ArrayUtils.isEmpty(fileStatus)) {
        long modifiedMS = fileStatus[0].getModificationTime();
        if (lastCandidatesUpdateMS.get() != modifiedMS) {
          updateClientConfInternal();
          lastCandidatesUpdateMS.set(modifiedMS);
          LOG.info("Update client conf from {} successfully.", path);
        }
      } else {
        LOG.warn("Client conf file not found with {}", path);
      }
    } catch (Exception e) {
      LOG.warn("Error when update client conf with " + path, e);
    }
  }

  private void updateClientConfInternal() {
    Map<String, String> newClientConf = Maps.newConcurrentMap();
    String content = loadClientConfContent();
    if (StringUtils.isEmpty(content)) {
      clientConf = newClientConf;
      LOG.warn("Load empty content from {}, ignore this updating.", path.toUri().toString());
      return;
    }

    boolean hasRemoteStorageConf = false;
    String remoteStoragePath = "";
    String remoteStorageConf = "";
    for (String item : content.split(IOUtils.LINE_SEPARATOR_UNIX)) {
      String confItem = item.trim();
      if (!StringUtils.isEmpty(confItem)) {
        String[] confKV = confItem.split(WHITESPACE_REGEX);
        if (confKV.length == 2) {
          if (CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key().equals(confKV[0])) {
            hasRemoteStorageConf = true;
            remoteStoragePath = confKV[1];
          } else if (CoordinatorConf.COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF.key().equals(confKV[0])) {
            remoteStorageConf = confKV[1];
          } else {
            newClientConf.put(confKV[0], confKV[1]);
          }
        }
      }
    }
    if (hasRemoteStorageConf) {
      applicationManager.refreshRemoteStorage(remoteStoragePath, remoteStorageConf);
    }

    clientConf = newClientConf;
  }

  private String loadClientConfContent() {
    String content = null;
    try (FSDataInputStream in = fileSystem.open(path)) {
      content = IOUtils.toString(in, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Fail to load content from {}", path.toUri().toString());
    }
    return content;
  }

  public Map<String, String> getClientConf() {
    return clientConf;
  }

  @Override
  public void close() {
    if (updateClientConfSES != null) {
      updateClientConfSES.shutdownNow();
    }
  }
}
