/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.connector.hadoop.util;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.constants.Constants;
import com.bytedance.bitsail.common.exception.CommonErrorCode;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.AttemptTimeLimiters;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HdfsUtils {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsUtils.class);
  private static final int HDFS_COMMIT_RETRY_NUM = 100;
  private static final int RETRY_DURATION_DEFAULT_VALUE = 10;
  static final Retryer<Object> COMMIT_RETRYER = RetryerBuilder.newBuilder()
      .retryIfException()
      .retryIfResult((res) -> !((Boolean) res))
      .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(RETRY_DURATION_DEFAULT_VALUE, TimeUnit.MINUTES))
      .withRetryListener(new RetryListener() {
        @Override
        public <V> void onRetry(Attempt<V> attempt) {
          if (attempt.hasException()) {
            LOG.error("Retry hdfs operation failed.", attempt.getExceptionCause());
          }
        }
      })
      .withWaitStrategy(WaitStrategies.fixedWait(Constants.RETRY_DELAY, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(HDFS_COMMIT_RETRY_NUM))
      .build();
  private static final int TIMEOUT_FUSE_RETRY_NUM = 1;

  private static final Retryer<Object> TIMEOUT_FUSE_RETRYER = RetryerBuilder.newBuilder()
      .retryIfException()
      .withRetryListener(new RetryListener() {
        @Override
        public <V> void onRetry(Attempt<V> attempt) {
          if (attempt.hasException()) {
            LOG.error("Retry hdfs operation failed.", attempt.getExceptionCause());
          }
        }
      })
      .withAttemptTimeLimiter(AttemptTimeLimiters.fixedTimeLimit(RETRY_DURATION_DEFAULT_VALUE, TimeUnit.MINUTES))
      .withWaitStrategy(WaitStrategies.fixedWait(Constants.RETRY_DELAY, TimeUnit.MILLISECONDS))
      .withStopStrategy(StopStrategies.stopAfterAttempt(TIMEOUT_FUSE_RETRY_NUM))
      .build();

  @VisibleForTesting
  static <T> T hdfsRetry(Retryer<T> retryer, final Callable<T> lambda) throws IOException {
    try {
      return retryer.call(lambda);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  public static boolean checkExists(Path path) throws IOException {
    Callable<Object> checkExists = () -> {
      long st = System.currentTimeMillis();
      boolean ret = path.getFileSystem().exists(path);
      LOG.info("[HDFS Operation] Check " + path + " finished. result: " + ret + ". Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return ret;
    };
    return (Boolean) hdfsRetry(TIMEOUT_FUSE_RETRYER, checkExists);
  }

  public static boolean deletePath(Path path) throws IOException {
    Callable<Object> deletePath = () -> {
      long st = System.currentTimeMillis();
      boolean result = path.getFileSystem().delete(path, true);
      LOG.info("[HDFS Operation] Delete {} finished. result: {}. Taken: {}.",
          path, result, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return result;
    };
    return (Boolean) hdfsRetry(TIMEOUT_FUSE_RETRYER, deletePath);
  }

  public static void touchEmptyFile(Path f) throws IOException {
    Callable<Object> operation = () -> {
      long st = System.currentTimeMillis();
      FSDataOutputStream fsDataOutputStream = f.getFileSystem().create(f, true);
      LOG.info("[HDFS Operation] Touch " + f + " finished. Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      fsDataOutputStream.close();
      return null;
    };
    hdfsRetry(TIMEOUT_FUSE_RETRYER, operation);
  }

  public static boolean rename(Path src, Path dst, boolean checkExist) throws IOException {
    Callable<Object> operation = () -> {
      FileSystem fs = src.getFileSystem();
      if (checkExist && fs.exists(dst)) {
        throw BitSailException.asBitSailException(CommonErrorCode.HDFS_EXCEPTION, "dst " + dst.getPath() + " is existing.");
      }
      long st = System.currentTimeMillis();
      boolean result = fs.rename(src, dst);
      LOG.info("[HDFS Operation] Rename " + src + " to " + dst + " finished, result: " + result + ". Taken: " +
          TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return result;
    };
    return (Boolean) hdfsRetry(TIMEOUT_FUSE_RETRYER, operation);
  }

  public static FileStatus getFileStatus(Path filePath) throws IOException {
    Callable<Object> operation = () -> {
      long st = System.currentTimeMillis();
      FileSystem fileSystem = filePath.getFileSystem();
      FileStatus fileStatus = fileSystem.getFileStatus(filePath);
      LOG.info("[HDFS Operation] getFileStatus " + filePath + " finished. Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return fileStatus;
    };
    return (FileStatus) hdfsRetry(TIMEOUT_FUSE_RETRYER, operation);
  }

  public static FileStatus[] listStatus(Path filePath) throws IOException {
    Callable<Object> operation = () -> {
      long st = System.currentTimeMillis();
      FileSystem fileSystem = filePath.getFileSystem();
      FileStatus[] fileStatuses = fileSystem.listStatus(filePath);
      LOG.info("[HDFS Operation] listStatus " + filePath + " finished. Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return fileStatuses;
    };
    return (FileStatus[]) hdfsRetry(TIMEOUT_FUSE_RETRYER, operation);
  }

  public static boolean mkdir(Path dirPath) throws IOException {
    Callable<Object> operation = () -> {
      long st = System.currentTimeMillis();
      FileSystem fs = dirPath.getFileSystem();
      boolean ret = fs.mkdirs(dirPath);
      LOG.info(
          "[HDFS Operation] mkdir " + dirPath + " finished. result: " + ret + " Taken: " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - st) + " sec.");
      return ret;
    };
    return (Boolean) hdfsRetry(TIMEOUT_FUSE_RETRYER, operation);
  }

  /**
   * Calculate the file size which under the location.
   */
  public static long getFileSize(Path filePath) throws IOException {
    if (!checkExists(filePath)) {
      LOG.warn("current file: {} does not exist, file size should be 0", filePath);
      return 0;
    }
    FileStatus fileStatus = getFileStatus(filePath);
    Queue<FileStatus> fileStatusQueue = new LinkedList<>();
    fileStatusQueue.add(fileStatus);
    long len = 0;
    while (!fileStatusQueue.isEmpty()) {
      FileStatus fs = fileStatusQueue.poll();
      if (!fs.isDir()) {
        len += fs.getLen();
      } else {
        Path path = fs.getPath();
        FileStatus[] fileStatuses = listStatus(path);
        for (FileStatus fileStatusInstance : fileStatuses) {
          fileStatusQueue.offer(fileStatusInstance);
        }
      }
    }
    return len;
  }

  public static void renameIfNotExist(Path src, Path dst) throws ExecutionException, RetryException {
    COMMIT_RETRYER.call(() -> {
      boolean success = false;
      try {
        success = success || HdfsUtils.rename(src, dst, false);
      } catch (Throwable t) {
        //Ignore
      }
      success = success || HdfsUtils.checkExists(dst);
      return success;
    });
  }

  public static void deleteIfExist(Path path) throws ExecutionException, RetryException, IOException {
    COMMIT_RETRYER.call(() -> {
      boolean exists = HdfsUtils.checkExists(path);
      if (!exists) {
        return true;
      }
      return HdfsUtils.deletePath(path);
    });
  }
}
