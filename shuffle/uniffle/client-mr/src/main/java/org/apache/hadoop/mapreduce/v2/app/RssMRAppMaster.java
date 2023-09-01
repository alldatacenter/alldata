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

package org.apache.hadoop.mapreduce.v2.app;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.ipc.CallerContext;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.JobSubmissionFiles;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.RssMRConfig;
import org.apache.hadoop.mapreduce.RssMRUtils;
import org.apache.hadoop.mapreduce.v2.app.client.ClientService;
import org.apache.hadoop.mapreduce.v2.app.job.Job;
import org.apache.hadoop.mapreduce.v2.app.job.impl.JobImpl;
import org.apache.hadoop.mapreduce.v2.app.local.LocalContainerAllocator;
import org.apache.hadoop.mapreduce.v2.app.rm.ContainerAllocator;
import org.apache.hadoop.mapreduce.v2.app.rm.ContainerAllocatorEvent;
import org.apache.hadoop.mapreduce.v2.app.rm.RMCommunicator;
import org.apache.hadoop.mapreduce.v2.app.rm.RMContainerAllocator;
import org.apache.hadoop.mapreduce.v2.app.rm.RMHeartbeatHandler;
import org.apache.hadoop.mapreduce.v2.util.MRApps;
import org.apache.hadoop.mapreduce.v2.util.MRWebAppUtil;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.service.ServiceOperations;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.SystemClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.client.api.ShuffleWriteClient;
import org.apache.uniffle.client.util.ClientUtils;
import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.common.RemoteStorageInfo;
import org.apache.uniffle.common.ShuffleAssignmentsInfo;
import org.apache.uniffle.common.ShuffleDataDistributionType;
import org.apache.uniffle.common.ShuffleServerInfo;
import org.apache.uniffle.common.exception.RssException;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.common.util.RetryUtils;
import org.apache.uniffle.storage.util.StorageType;

public class RssMRAppMaster extends MRAppMaster {

  private final String rssNmHost;
  private final int rssNmPort;
  private final int rssNmHttpPort;
  private final ContainerId rssContainerID;
  private RssContainerAllocatorRouter rssContainerAllocator;

  public RssMRAppMaster(
      ApplicationAttemptId applicationAttemptId,
      ContainerId containerId,
      String nmHost,
      int nmPort,
      int nmHttpPort,
      long appSubmitTime) {
    super(applicationAttemptId, containerId, nmHost, nmPort, nmHttpPort, new SystemClock(), appSubmitTime);
    rssNmHost = nmHost;
    rssNmPort = nmPort;
    rssNmHttpPort = nmHttpPort;
    rssContainerID = containerId;
    rssContainerAllocator = null;
  }

  private static final Logger LOG = LoggerFactory.getLogger(RssMRAppMaster.class);

  public static void main(String[] args) {

    JobConf conf = new JobConf(new YarnConfiguration());
    conf.addResource(new Path(MRJobConfig.JOB_CONF_FILE));

    int numReduceTasks = conf.getInt(MRJobConfig.NUM_REDUCES, 0);
    if (numReduceTasks > 0) {
      String coordinators = conf.get(RssMRConfig.RSS_COORDINATOR_QUORUM);

      ShuffleWriteClient client = RssMRUtils.createShuffleClient(conf);

      LOG.info("Registering coordinators {}", coordinators);
      client.registerCoordinators(coordinators);

      // Get the configured server assignment tags and it will also add default shuffle version tag.
      Set<String> assignmentTags = new HashSet<>();
      String rawTags = conf.get(RssMRConfig.RSS_CLIENT_ASSIGNMENT_TAGS, "");
      if (StringUtils.isNotEmpty(rawTags)) {
        rawTags = rawTags.trim();
        assignmentTags.addAll(Arrays.asList(rawTags.split(",")));
      }
      assignmentTags.add(Constants.SHUFFLE_SERVER_VERSION);

      final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
              Thread t = Executors.defaultThreadFactory().newThread(r);
              t.setDaemon(true);
              return t;
            }
          }
      );

      JobConf extraConf = new JobConf();
      extraConf.clear();

      // get remote storage from coordinator if necessary
      boolean dynamicConfEnabled = conf.getBoolean(RssMRConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED,
          RssMRConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED_DEFAULT_VALUE);

      // fetch client conf and apply them if necessary
      if (dynamicConfEnabled) {
        Map<String, String> clusterClientConf = client.fetchClientConf(
            conf.getInt(RssMRConfig.RSS_ACCESS_TIMEOUT_MS,
                RssMRConfig.RSS_ACCESS_TIMEOUT_MS_DEFAULT_VALUE));
        RssMRUtils.applyDynamicClientConf(extraConf, clusterClientConf);
      }

      String storageType = RssMRUtils.getString(extraConf, conf, RssMRConfig.RSS_STORAGE_TYPE);
      boolean testMode = RssMRUtils.getBoolean(extraConf, conf, RssMRConfig.RSS_TEST_MODE_ENABLE, false);
      ClientUtils.validateTestModeConf(testMode, storageType);
      ApplicationAttemptId applicationAttemptId = RssMRUtils.getApplicationAttemptId();
      String appId = applicationAttemptId.toString();
      RemoteStorageInfo defaultRemoteStorage =
          new RemoteStorageInfo(conf.get(RssMRConfig.RSS_REMOTE_STORAGE_PATH, ""));
      RemoteStorageInfo remoteStorage = ClientUtils.fetchRemoteStorage(
          appId, defaultRemoteStorage, dynamicConfEnabled, storageType, client);
      // set the remote storage with actual value
      extraConf.set(RssMRConfig.RSS_REMOTE_STORAGE_PATH, remoteStorage.getPath());
      extraConf.set(RssMRConfig.RSS_REMOTE_STORAGE_CONF, remoteStorage.getConfString());
      RssMRUtils.validateRssClientConf(extraConf, conf);
      // When containers have disk with very limited space, reduce is allowed to spill data to hdfs
      if (conf.getBoolean(RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ENABLED,
          RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ENABLED_DEFAULT)) {

        if (remoteStorage.isEmpty()) {
          throw new IllegalArgumentException("Remote spill only supports "
            + StorageType.MEMORY_LOCALFILE_HDFS.name() + " mode with " + remoteStorage);
        }

        // When remote spill is enabled, reduce task is more easy to crash.
        // We allow more attempts to avoid recomputing job.
        int originalAttempts = conf.getInt(MRJobConfig.REDUCE_MAX_ATTEMPTS, 4);
        int inc = conf.getInt(RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ATTEMPT_INC,
            RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ATTEMPT_INC_DEFAULT);
        if (inc < 0) {
          throw new IllegalArgumentException(RssMRConfig.RSS_REDUCE_REMOTE_SPILL_ATTEMPT_INC
              + " cannot be negative");
        }
        conf.setInt(MRJobConfig.REDUCE_MAX_ATTEMPTS, originalAttempts + inc);
      }
      
      int requiredAssignmentShuffleServersNum = RssMRUtils.getRequiredShuffleServerNumber(conf);
      // retryInterval must bigger than `rss.server.heartbeat.timeout`, or maybe it will return the same result
      long retryInterval = conf.getLong(RssMRConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL,
              RssMRConfig.RSS_CLIENT_ASSIGNMENT_RETRY_INTERVAL_DEFAULT_VALUE);
      int retryTimes = conf.getInt(RssMRConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES,
              RssMRConfig.RSS_CLIENT_ASSIGNMENT_RETRY_TIMES_DEFAULT_VALUE);
      ShuffleAssignmentsInfo response;
      try {
        response = RetryUtils.retry(() -> {
          ShuffleAssignmentsInfo shuffleAssignments =
                  client.getShuffleAssignments(
                          appId,
                          0,
                          numReduceTasks,
                          1,
                          Sets.newHashSet(assignmentTags),
                          requiredAssignmentShuffleServersNum,
                          -1
                  );

          Map<ShuffleServerInfo, List<PartitionRange>> serverToPartitionRanges =
              shuffleAssignments.getServerToPartitionRanges();

          if (serverToPartitionRanges == null || serverToPartitionRanges.isEmpty()) {
            return null;
          }
          LOG.info("Start to register shuffle");
          long start = System.currentTimeMillis();
          serverToPartitionRanges.entrySet().forEach(entry -> client.registerShuffle(
              entry.getKey(),
              appId,
              0,
              entry.getValue(),
              remoteStorage,
              ShuffleDataDistributionType.NORMAL
          ));
          LOG.info("Finish register shuffle with " + (System.currentTimeMillis() - start) + " ms");
          return shuffleAssignments;
        }, retryInterval, retryTimes);
      } catch (Throwable throwable) {
        throw new RssException("registerShuffle failed!", throwable);
      }

      if (response == null) {
        return;
      }
      long heartbeatInterval = conf.getLong(RssMRConfig.RSS_HEARTBEAT_INTERVAL,
          RssMRConfig.RSS_HEARTBEAT_INTERVAL_DEFAULT_VALUE);
      long heartbeatTimeout = conf.getLong(RssMRConfig.RSS_HEARTBEAT_TIMEOUT, heartbeatInterval / 2);
      client.registerApplicationInfo(appId, heartbeatTimeout, "user");
      scheduledExecutorService.scheduleAtFixedRate(
          () -> {
            try {
              client.sendAppHeartbeat(appId, heartbeatTimeout);
              LOG.info("Finish send heartbeat to coordinator and servers");
            } catch (Exception e) {
              LOG.warn("Fail to send heartbeat to coordinator and servers", e);
            }
          },
          heartbeatInterval / 2,
          heartbeatInterval,
          TimeUnit.MILLISECONDS);

      // write shuffle worker assignments to submit work directory
      // format is as below:
      // mapreduce.rss.assignment.partition.1:server1,server2
      // mapreduce.rss.assignment.partition.2:server3,server4
      // ...
      response.getPartitionToServers().entrySet().forEach(entry -> {
        List<String> servers = Lists.newArrayList();
        for (ShuffleServerInfo server : entry.getValue()) {
          servers.add(server.getHost() + ":" + server.getPort());
        }
        extraConf.set(RssMRConfig.RSS_ASSIGNMENT_PREFIX + entry.getKey(), StringUtils.join(servers, ","));
      });

      writeExtraConf(conf, extraConf);

      // close slow start
      conf.setFloat(MRJobConfig.COMPLETED_MAPS_FOR_REDUCE_SLOWSTART, 1.0f);
      LOG.warn("close slow start, because RSS does not support it yet");

      // MapReduce don't set setKeepContainersAcrossApplicationAttempts in AppContext, there will be no container
      // to be shared between attempts. Rss don't support shared container between attempts.
      conf.setBoolean(MRJobConfig.MR_AM_JOB_RECOVERY_ENABLE, false);
      LOG.warn("close recovery enable, because RSS doesn't support it yet");

      String jobDirStr = conf.get(MRJobConfig.MAPREDUCE_JOB_DIR);
      if (jobDirStr == null) {
        throw new RuntimeException("jobDir is empty");
      }
    }
    try {
      setMainStartedTrue();
      Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
      String containerIdStr = System.getenv(ApplicationConstants.Environment.CONTAINER_ID.name());
      validateInputParam(containerIdStr, ApplicationConstants.Environment.CONTAINER_ID.name());
      String nodeHostString = System.getenv(ApplicationConstants.Environment.NM_HOST.name());
      validateInputParam(nodeHostString, ApplicationConstants.Environment.NM_HOST.name());
      String nodePortString = System.getenv(ApplicationConstants.Environment.NM_PORT.name());
      validateInputParam(nodePortString, ApplicationConstants.Environment.NM_PORT.name());
      String nodeHttpPortString = System.getenv(ApplicationConstants.Environment.NM_HTTP_PORT.name());
      validateInputParam(nodeHttpPortString, ApplicationConstants.Environment.NM_HTTP_PORT.name());
      String appSubmitTimeStr = System.getenv("APP_SUBMIT_TIME_ENV");
      validateInputParam(appSubmitTimeStr, "APP_SUBMIT_TIME_ENV");
      ContainerId containerId = ContainerId.fromString(containerIdStr);
      ApplicationAttemptId applicationAttemptId = containerId.getApplicationAttemptId();
      if (applicationAttemptId != null) {
        CallerContext.setCurrent((
            new CallerContext.Builder("mr_appmaster_" + applicationAttemptId.toString())).build());
      }

      long appSubmitTime = Long.parseLong(appSubmitTimeStr);
      RssMRAppMaster appMaster = new RssMRAppMaster(
          applicationAttemptId, containerId, nodeHostString, Integer.parseInt(nodePortString),
          Integer.parseInt(nodeHttpPortString), appSubmitTime);
      ShutdownHookManager.get().addShutdownHook(new RssMRAppMasterShutdownHook(appMaster), 30);
      MRWebAppUtil.initialize(conf);
      String systemPropsToLog = MRApps.getSystemPropertiesToLog(conf);
      if (systemPropsToLog != null) {
        LOG.info(systemPropsToLog);
      }
      String jobUserName = System.getenv(ApplicationConstants.Environment.USER.name());
      conf.set("mapreduce.job.user.name", jobUserName);
      initAndStartAppMaster(appMaster, conf, jobUserName);
    } catch (Throwable t) {
      LOG.error("Error starting MRAppMaster", t);
      ExitUtil.terminate(1, t);
    }
  }

  private static void setMainStartedTrue() throws Exception {
    Field field = MRAppMaster.class.getDeclaredField("mainStarted");
    field.setAccessible(true);
    field.setBoolean(null, true);
    field.setAccessible(false);
  }

  protected ContainerAllocator createContainerAllocator(ClientService clientService, AppContext context) {
    rssContainerAllocator = new RssContainerAllocatorRouter(clientService, context);
    return rssContainerAllocator;
  }

  private static void validateInputParam(String value, String param) throws IOException {
    if (value == null) {
      String msg = param + " is null";
      LOG.error(msg);
      throw new IOException(msg);
    }
  }

  static void writeExtraConf(JobConf conf, JobConf extraConf) {
    try {
      FileSystem fs = new Cluster(conf).getFileSystem();
      String jobDirStr = conf.get(MRJobConfig.MAPREDUCE_JOB_DIR);
      Path assignmentFile = new Path(jobDirStr, RssMRConfig.RSS_CONF_FILE);

      try (FSDataOutputStream out =
               FileSystem.create(fs, assignmentFile,
                   new FsPermission(JobSubmissionFiles.JOB_FILE_PERMISSION))) {
        extraConf.writeXml(out);
      }
      FileStatus status = fs.getFileStatus(assignmentFile);
      long currentTs = status.getModificationTime();
      String uri = fs.getUri() + Path.SEPARATOR + assignmentFile.toUri();
      String files = conf.get(MRJobConfig.CACHE_FILES);
      conf.set(MRJobConfig.CACHE_FILES, files == null ? uri : uri + "," + files);
      String ts = conf.get(MRJobConfig.CACHE_FILE_TIMESTAMPS);
      conf.set(MRJobConfig.CACHE_FILE_TIMESTAMPS,
          ts == null ? String.valueOf(currentTs) : currentTs + "," + ts);
      String vis = conf.get(MRJobConfig.CACHE_FILE_VISIBILITIES);
      conf.set(MRJobConfig.CACHE_FILE_VISIBILITIES, vis == null ? "false" : "false" + "," + vis);
      long size = status.getLen();
      String sizes = conf.get(MRJobConfig.CACHE_FILES_SIZES);
      conf.set(MRJobConfig.CACHE_FILES_SIZES, sizes == null ? String.valueOf(size) : size + "," + sizes);
    } catch (InterruptedException | IOException e) {
      LOG.error("Upload extra conf exception", e);
      throw new RuntimeException("Upload extra conf exception ", e);
    }
  }

  private final class RssContainerAllocatorRouter
      extends AbstractService implements ContainerAllocator, RMHeartbeatHandler {
    private final ClientService clientService;
    private final AppContext context;
    private ContainerAllocator containerAllocator;

    RssContainerAllocatorRouter(ClientService clientService, AppContext context) {
      super(RssMRAppMaster.RssContainerAllocatorRouter.class.getName());
      this.clientService = clientService;
      this.context = context;
    }

    protected void serviceStart() throws Exception {
      if (RssMRAppMaster.this.getJob().isUber()) {
        MRApps.setupDistributedCacheLocal(this.getConfig());
        this.containerAllocator = new LocalContainerAllocator(
            this.clientService,
            this.context,
            RssMRAppMaster.this.rssNmHost,
            RssMRAppMaster.this.rssNmPort,
            RssMRAppMaster.this.rssNmHttpPort,
            RssMRAppMaster.this.rssContainerID);
      } else {
        this.containerAllocator = new RMContainerAllocator(this.clientService, this.context) {
          @Override
          protected AllocateResponse makeRemoteRequest() throws YarnException, IOException {
            AllocateResponse response = super.makeRemoteRequest();
            // UpdateNodes only have one use for MRAppMaster, MRAppMaster use the updateNodes to find which
            // nodes are bad nodes. So we clear them, MRAppMaster will not recompute the map tasks.
            response.getUpdatedNodes().clear();
            return response;
          }
        };
      }

      ((Service)this.containerAllocator).init(this.getConfig());
      ((Service)this.containerAllocator).start();
      super.serviceStart();
    }

    protected void serviceStop() throws Exception {
      ServiceOperations.stop((Service)this.containerAllocator);
      super.serviceStop();
    }

    public void handle(ContainerAllocatorEvent event) {
      this.containerAllocator.handle(event);
    }

    public void setSignalled(boolean isSignalled) {
      ((RMCommunicator)this.containerAllocator).setSignalled(isSignalled);
    }

    public void setShouldUnregister(boolean shouldUnregister) {
      ((RMCommunicator)this.containerAllocator).setShouldUnregister(shouldUnregister);
    }

    public long getLastHeartbeatTime() {
      return ((RMCommunicator)this.containerAllocator).getLastHeartbeatTime();
    }

    public void runOnNextHeartbeat(Runnable callback) {
      ((RMCommunicator)this.containerAllocator).runOnNextHeartbeat(callback);
    }
  }

  @Override
  public void notifyIsLastAMRetry(boolean isLastAMRetry) {
    LOG.info("Notify RMCommunicator isAMLastRetry: " + isLastAMRetry);
    if (rssContainerAllocator != null) {
      rssContainerAllocator.setShouldUnregister(isLastAMRetry);
    }
    super.notifyIsLastAMRetry(isLastAMRetry);
  }

  static class RssMRAppMasterShutdownHook implements Runnable {
    RssMRAppMaster appMaster;

    RssMRAppMasterShutdownHook(RssMRAppMaster appMaster) {
      this.appMaster = appMaster;
    }

    public void run() {
      RssMRAppMaster.LOG.info("MRAppMaster received a signal. Signaling RMCommunicator and JobHistoryEventHandler.");
      RssContainerAllocatorRouter allocatorRouter = this.appMaster.rssContainerAllocator;
      if (allocatorRouter != null) {
        allocatorRouter.setSignalled(true);
      }
      this.appMaster.notifyIsLastAMRetry(this.appMaster.isLastAMRetry);
      this.appMaster.stop();
    }
  }

  private Job getJob() {
    try {
      Field field = RssMRAppMaster.class.getSuperclass().getDeclaredField("job");
      field.setAccessible(true);
      JobImpl job = (JobImpl)field.get(this);
      field.setAccessible(false);
      return job;
    } catch (Exception e) {
      LOG.error("getJob error !" + e.getMessage());
      return null;
    }
  }
}
