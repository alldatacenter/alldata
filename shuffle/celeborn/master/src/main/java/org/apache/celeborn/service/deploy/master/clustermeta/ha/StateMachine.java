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

package org.apache.celeborn.service.deploy.master.clustermeta.ha;

import static org.apache.ratis.util.LifeCycle.State.PAUSED;
import static org.apache.ratis.util.LifeCycle.State.PAUSING;
import static org.apache.ratis.util.LifeCycle.State.RUNNING;
import static org.apache.ratis.util.LifeCycle.State.STARTING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.ratis.io.MD5Hash;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.raftlog.RaftLog;
import org.apache.ratis.server.storage.FileInfo;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.SnapshotInfo;
import org.apache.ratis.statemachine.SnapshotRetentionPolicy;
import org.apache.ratis.statemachine.StateMachineStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.statemachine.impl.SingleFileSnapshotInfo;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.apache.ratis.util.ExitUtils;
import org.apache.ratis.util.FileUtils;
import org.apache.ratis.util.LifeCycle;
import org.apache.ratis.util.MD5FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.util.ThreadUtils;
import org.apache.celeborn.service.deploy.master.clustermeta.ResourceProtos;
import org.apache.celeborn.service.deploy.master.clustermeta.ResourceProtos.ResourceResponse;

public class StateMachine extends BaseStateMachine {
  private static final Logger LOG = LoggerFactory.getLogger(StateMachine.class);

  public static final Pattern MD5_REGEX = Pattern.compile("snapshot\\.(\\d+)_(\\d+)\\.md5");

  private final SimpleStateMachineStorage storage =
      new SimpleStateMachineStorage() {
        /**
         * we need to delete md5 file as the same time as snapshot file deleted, so we override the
         * SimpleStateMachineStorage.cleanupOldSnapshots method, add delete md5 file action.
         *
         * @param snapshotRetentionPolicy snapshot retention policy
         * @throws IOException
         */
        @Override
        public void cleanupOldSnapshots(SnapshotRetentionPolicy snapshotRetentionPolicy)
            throws IOException {
          if (snapshotRetentionPolicy != null
              && snapshotRetentionPolicy.getNumSnapshotsRetained() > 0) {
            List<SingleFileSnapshotInfo> allSnapshotFiles = new ArrayList<>();
            List<SingleFileSnapshotInfo> allMD5Files = new ArrayList<>();
            try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(this.getSmDir().toPath())) {
              for (Path path : stream) {
                if (filePatternMatches(SNAPSHOT_REGEX, allSnapshotFiles, path)) {
                  continue;
                } else {
                  filePatternMatches(MD5_REGEX, allMD5Files, path);
                }
              }
            }
            // first step, cleanup old snapshot and md5 file
            SingleFileSnapshotInfo snapshotInfo =
                cleanupOldFiles(
                    allSnapshotFiles,
                    snapshotRetentionPolicy.getNumSnapshotsRetained(),
                    false,
                    null);
            // second step, cleanup only old md5 file
            cleanupOldFiles(
                allMD5Files, snapshotRetentionPolicy.getNumSnapshotsRetained(), true, snapshotInfo);
          }
        }

        private boolean filePatternMatches(
            Pattern pattern, List<SingleFileSnapshotInfo> result, Path filePath) {
          Matcher md5Matcher = pattern.matcher(filePath.getFileName().toString());
          if (md5Matcher.matches()) {
            final long endIndex = Long.parseLong(md5Matcher.group(2));
            final long term = Long.parseLong(md5Matcher.group(1));
            final FileInfo fileInfo = new FileInfo(filePath, null);
            result.add(new SingleFileSnapshotInfo(fileInfo, term, endIndex));
            return true;
          }
          return false;
        }

        private SingleFileSnapshotInfo cleanupOldFiles(
            List<SingleFileSnapshotInfo> inputFiles,
            int retainedNum,
            boolean onlyCleanupMD5Files,
            SingleFileSnapshotInfo snapshotInfo) {
          SingleFileSnapshotInfo result = null;
          if (inputFiles.size() > retainedNum) {
            inputFiles.sort(new RatisSnapshotFileComparator());
            List<SingleFileSnapshotInfo> filesToBeCleaned =
                inputFiles.subList(retainedNum, inputFiles.size());
            result = filesToBeCleaned.get(0);
            for (SingleFileSnapshotInfo fileInfo : filesToBeCleaned) {
              if ((null != snapshotInfo && (fileInfo.getIndex() >= snapshotInfo.getIndex())
                  || (onlyCleanupMD5Files && null == snapshotInfo))) {
                continue;
              }
              File file = fileInfo.getFile().getPath().toFile();
              if (onlyCleanupMD5Files) {
                LOG.info("Deleting old md5 file at {}.", file.getAbsolutePath());
                FileUtils.deleteFileQuietly(file);
              } else {
                File md5File = new File(file.getAbsolutePath() + MD5FileUtil.MD5_SUFFIX);
                LOG.info(
                    "Deleting old snapshot at {}, md5 file at {}.",
                    file.getAbsolutePath(),
                    md5File.getAbsolutePath());
                FileUtils.deleteFileQuietly(file);
                FileUtils.deleteFileQuietly(md5File);
              }
            }
          }
          return result;
        }
      };

  private final HARaftServer masterRatisServer;
  private RaftGroupId raftGroupId;
  private final ExecutorService executorService;

  private RaftServer mServer;

  private final MetaHandler metaHandler;

  public StateMachine(HARaftServer ratisServer) {
    this.masterRatisServer = ratisServer;
    this.metaHandler = ratisServer.getMetaHandler();

    this.executorService = ThreadUtils.newDaemonSingleThreadExecutor("Master-Meta-StateMachine");
  }

  /** Initializes the State Machine with the given server, group and storage. */
  @Override
  public void initialize(RaftServer server, RaftGroupId id, RaftStorage raftStorage)
      throws IOException {
    getLifeCycle()
        .startAndTransition(
            () -> {
              super.initialize(server, id, raftStorage);
              this.mServer = server;
              this.raftGroupId = id;
              storage.init(raftStorage);
            });
    loadSnapshot(storage.getLatestSnapshot());
    LOG.info("Initialized State Machine.");
  }

  @Override
  public void reinitialize() throws IOException {
    LOG.info("Reinitializing state machine.");
    getLifeCycle().compareAndTransition(PAUSED, STARTING);
    storage.loadLatestSnapshot();
    loadSnapshot(storage.getLatestSnapshot());
    getLifeCycle().compareAndTransition(STARTING, RUNNING);
  }

  @Override
  public void pause() {
    getLifeCycle().compareAndTransition(RUNNING, PAUSING);
    getLifeCycle().compareAndTransition(PAUSING, PAUSED);
  }

  private synchronized void loadSnapshot(SingleFileSnapshotInfo snapshot) throws IOException {
    if (snapshot == null) {
      return;
    }
    if (snapshot.getTermIndex().compareTo(getLastAppliedTermIndex()) <= 0) {
      LOG.info("obsolete snapshot provided: {}", snapshot.getTermIndex());
      return;
    }
    LOG.info("Loading Snapshot {}.", snapshot);
    final File snapshotFile = snapshot.getFile().getPath().toFile();
    if (!snapshotFile.exists()) {
      throw new FileNotFoundException(
          String.format("The snapshot file %s does not exist", snapshotFile.getPath()));
    }
    try {
      setLastAppliedTermIndex(snapshot.getTermIndex());
      install(snapshotFile);
    } catch (IOException rethrow) {
      LOG.error("Failed to load snapshot {}", snapshot);
      throw rethrow;
    }
  }

  private void install(File snapshotFile) throws IOException {
    try {
      metaHandler.loadSnapShot(snapshotFile);
    } catch (IOException rethrow) {
      LOG.warn("Failed to install snapshot!", rethrow);
      throw rethrow;
    }
    LOG.info("Successfully installed snapshot!");
  }

  @Override
  public SnapshotInfo getLatestSnapshot() {
    return storage.getLatestSnapshot();
  }

  @Override
  public TransactionContext startTransaction(RaftClientRequest raftClientRequest)
      throws IOException {
    Preconditions.checkArgument(raftClientRequest.getRaftGroupId().equals(raftGroupId));
    return handleStartTransactionRequests(raftClientRequest);
  }

  /** Apply a committed log entry to the state machine. */
  @Override
  public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
    try {
      ResourceProtos.ResourceRequest request =
          HAHelper.convertByteStringToRequest(trx.getStateMachineLogEntry().getLogData());
      long trxLogIndex = trx.getLogEntry().getIndex();
      // In the current approach we have one single global thread executor.
      // with single thread. Right now this is being done for correctness, as
      // applyTransaction will be run on multiple Master we want to execute the
      // transactions in the same order on all Master, otherwise there is a
      // chance that Master replica can be out of sync.
      // Ref: from Ozone project (OzoneManagerStateMachine)
      CompletableFuture<Message> ratisFuture = new CompletableFuture<>();
      CompletableFuture<ResourceResponse> future =
          CompletableFuture.supplyAsync(() -> runCommand(request, trxLogIndex), executorService);
      future.thenApply(
          response -> {
            if (!response.getSuccess()) {
              LOG.warn(
                  "Failed to apply log {} for this raft group {}!",
                  request.getCmdType(),
                  this.raftGroupId);
            }

            byte[] responseBytes = response.toByteArray();
            ratisFuture.complete(Message.valueOf(ByteString.copyFrom(responseBytes)));
            return ratisFuture;
          });
      return ratisFuture;
    } catch (Exception e) {
      return completeExceptionally(e);
    }
  }

  /**
   * Submits write request to MetaSystem and returns the response Message.
   *
   * @param request MasterMetaRequest
   * @return response from meta system
   */
  @VisibleForTesting
  protected ResourceResponse runCommand(ResourceProtos.ResourceRequest request, long trxLogIndex) {
    try {
      return metaHandler.handleWriteRequest(request);
    } catch (Throwable e) {
      String errorMessage = "Request " + request + "failed with exception";
      ExitUtils.terminate(1, errorMessage, e, LOG);
    }
    return null;
  }

  /** Query the state machine. The request must be read-only. */
  @Override
  public CompletableFuture<Message> query(Message request) {
    try {
      byte[] bytes = request.getContent().toByteArray();
      return CompletableFuture.completedFuture(
          queryCommand(ResourceProtos.ResourceRequest.parseFrom(bytes)));
    } catch (IOException e) {
      return completeExceptionally(e);
    }
  }

  /**
   * Submits read request to MetaSystem and returns the response Message.
   *
   * @param request MasterMetaRequest
   * @return response from meta system
   */
  private Message queryCommand(ResourceProtos.ResourceRequest request) {
    ResourceResponse response = metaHandler.handleReadRequest(request);
    return HAHelper.convertResponseToMessage(response);
  }

  /**
   * Store the current state as an snapshot file in the stateMachineStorage.
   *
   * @return the index of the snapshot
   */
  @Override
  public long takeSnapshot() {
    if (mServer.getLifeCycleState() != LifeCycle.State.RUNNING) {
      LOG.warn(
          "Skip taking snapshot because raft server is not in running state: "
              + "current state is {}.",
          mServer.getLifeCycleState());
      return RaftLog.INVALID_LOG_INDEX;
    }
    TermIndex lastTermIndex = getLastAppliedTermIndex();
    LOG.debug("Current Snapshot Index {}.", lastTermIndex);
    File tempFile;
    try {
      tempFile = HAHelper.createTempSnapshotFile(storage);
      metaHandler.writeToSnapShot(tempFile);
    } catch (IOException e) {
      LOG.warn("Failed to create temp snapshot file.", e);
      return RaftLog.INVALID_LOG_INDEX;
    }
    LOG.debug("Taking a snapshot to file {}.", tempFile);
    final File snapshotFile =
        storage.getSnapshotFile(lastTermIndex.getTerm(), lastTermIndex.getIndex());
    try {
      final MD5Hash digest = MD5FileUtil.computeMd5ForFile(tempFile);
      LOG.info("Saving digest {} for snapshot file {}.", digest, snapshotFile);
      MD5FileUtil.saveMD5File(snapshotFile, digest);
      LOG.info("Renaming a snapshot file {} to {}.", tempFile, snapshotFile);
      if (!tempFile.renameTo(snapshotFile)) {
        tempFile.delete();
        LOG.warn("Failed to rename snapshot from {} to {}.", tempFile, snapshotFile);
        return RaftLog.INVALID_LOG_INDEX;
      }
      storage.loadLatestSnapshot();
    } catch (Exception e) {
      tempFile.delete();
      LOG.warn("Failed to complete snapshot: {}.", snapshotFile, e);
      return RaftLog.INVALID_LOG_INDEX;
    }
    return lastTermIndex.getIndex();
  }

  /** Notifies the state machine that the raft peer is no longer leader. */
  @Override
  public void notifyNotLeader(Collection<TransactionContext> pendingEntries) throws IOException {
    masterRatisServer.updateServerRole();
  }

  /**
   * Handle the RaftClientRequest and return TransactionContext object.
   *
   * @param raftClientRequest
   * @return TransactionContext
   */
  private TransactionContext handleStartTransactionRequests(RaftClientRequest raftClientRequest) {

    return TransactionContext.newBuilder()
        .setClientRequest(raftClientRequest)
        .setStateMachine(this)
        .setServerRole(RaftProtos.RaftPeerRole.LEADER)
        .setLogData(raftClientRequest.getMessage().getContent())
        .build();
  }

  @VisibleForTesting
  public void setRaftGroupId(RaftGroupId raftGroupId) {
    this.raftGroupId = raftGroupId;
  }

  private static <T> CompletableFuture<T> completeExceptionally(Exception e) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(e);
    return future;
  }

  @Override
  public StateMachineStorage getStateMachineStorage() {
    return this.storage;
  }
}

/**
 * Compare snapshot files based on transaction indexes. Copy from
 * org.apache.ratis.statemachine.impl.SnapshotFileComparator
 */
class RatisSnapshotFileComparator implements Comparator<SingleFileSnapshotInfo> {
  @Override
  public int compare(SingleFileSnapshotInfo file1, SingleFileSnapshotInfo file2) {
    return (int) (file2.getIndex() - file1.getIndex());
  }
}
