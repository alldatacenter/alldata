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

package org.apache.uniffle.coordinator.access.checker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Sets;
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
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.ThreadUtils;
import org.apache.uniffle.coordinator.AccessManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.access.AccessCheckResult;
import org.apache.uniffle.coordinator.access.AccessInfo;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

/**
 * AccessCandidatesChecker maintain a list of candidate access id and update it periodically,
 * it checks the access id in the access request and reject if the id is not in the candidate list.
 */
public class AccessCandidatesChecker extends AbstractAccessChecker {
  private static final Logger LOG = LoggerFactory.getLogger(AccessCandidatesChecker.class);

  private final AtomicReference<Set<String>> candidates = new AtomicReference<>();
  private final AtomicLong lastCandidatesUpdateMS = new AtomicLong(0L);
  private final Path path;
  private final ScheduledExecutorService updateAccessCandidatesSES;
  private final FileSystem fileSystem;

  public AccessCandidatesChecker(AccessManager accessManager) throws Exception {
    super(accessManager);
    CoordinatorConf conf = accessManager.getCoordinatorConf();
    String pathStr = conf.get(CoordinatorConf.COORDINATOR_ACCESS_CANDIDATES_PATH);
    this.path = new Path(pathStr);
    Configuration hadoopConf = accessManager.getHadoopConf();
    this.fileSystem = HadoopFilesystemProvider.getFilesystem(path, hadoopConf);

    if (!fileSystem.isFile(path)) {
      String msg = String.format("Fail to init AccessCandidatesChecker, %s is not a file.", path.toUri());
      LOG.error(msg);
      throw new RuntimeException(msg);
    }
    updateAccessCandidatesInternal();
    if (candidates.get() == null || candidates.get().isEmpty()) {
      String msg = "Candidates must be non-empty and can be loaded successfully at coordinator startup.";
      LOG.error(msg);
      throw new RuntimeException(msg);
    }
    LOG.debug("Load candidates: {}", String.join(";", candidates.get()));

    int updateIntervalS = conf.getInteger(CoordinatorConf.COORDINATOR_ACCESS_CANDIDATES_UPDATE_INTERVAL_SEC);
    updateAccessCandidatesSES = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("UpdateAccessCandidates-%d"));
    updateAccessCandidatesSES.scheduleAtFixedRate(
        this::updateAccessCandidates, 0, updateIntervalS, TimeUnit.SECONDS);
  }

  @Override
  public AccessCheckResult check(AccessInfo accessInfo) {
    String accessId = accessInfo.getAccessId().trim();
    if (!candidates.get().contains(accessId)) {
      String msg = String.format("Denied by AccessCandidatesChecker, accessInfo[%s].", accessInfo);
      LOG.debug("Candidates is {}, {}", candidates.get(), msg);
      CoordinatorMetrics.counterTotalCandidatesDeniedRequest.inc();
      return new AccessCheckResult(false, msg);
    }

    return new AccessCheckResult(true, Constants.COMMON_SUCCESS_MESSAGE);
  }

  @Override
  public void close() {
    if (updateAccessCandidatesSES != null) {
      updateAccessCandidatesSES.shutdownNow();
    }
  }

  private void updateAccessCandidates() {
    try {
      FileStatus[] fileStatus = fileSystem.listStatus(path);
      if (!ArrayUtils.isEmpty(fileStatus)) {
        long lastModifiedMS = fileStatus[0].getModificationTime();
        if (lastCandidatesUpdateMS.get() != lastModifiedMS) {
          updateAccessCandidatesInternal();
          lastCandidatesUpdateMS.set(lastModifiedMS);
          LOG.debug("Load candidates: {}", String.join(";", candidates.get()));
        }
      } else {
        LOG.warn("Candidates file not found.");
      }
      // TODO: add access num metrics
    } catch (Exception e) {
      LOG.warn("Error when update access candidates, ignore this updating.", e);
    }
  }

  private void updateAccessCandidatesInternal() {
    Set<String> newCandidates = Sets.newHashSet();
    String content = loadFileContent();
    if (StringUtils.isEmpty(content)) {
      LOG.warn("Load empty content from {}, ignore this updating", path.toUri().toString());
      return;
    }

    for (String item : content.split(IOUtils.LINE_SEPARATOR_UNIX)) {
      String accessId = item.trim();
      if (!StringUtils.isEmpty(accessId)) {
        newCandidates.add(accessId);
      }
    }

    if (newCandidates.isEmpty()) {
      LOG.warn("Empty content in {}, ignore this updating.", path.toUri().toString());
      return;
    }

    candidates.set(newCandidates);
  }

  private String loadFileContent() {
    String content = null;
    try (FSDataInputStream in = fileSystem.open(path)) {
      content = IOUtils.toString(in, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.error("Fail to load content from {}", path.toUri().toString());
    }
    return content;
  }

  public AtomicReference<Set<String>> getCandidates() {
    return candidates;
  }
}
