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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Thread.sleep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ViewDirectoryWatcher implements DirectoryWatcher {

  public static final int FIXED_FILE_COUNTER = 30;
  public static final int FILE_CHECK_INTERVAL_MILLIS = 200;
  // Global configuration
  @Inject
  Configuration configuration;

  // View Registry
  @Inject
  ViewRegistry viewRegistry;

  private WatchService watchService;

  // Executor service on which the watcher will run
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  private Future<?> watchTask;

  private static final Logger LOG = LoggerFactory.getLogger(ViewDirectoryWatcher.class);

  // Callbacks to hook into file processing
  private List<Function<Path, Boolean>> hooks = Lists.newArrayList(Collections.singleton(loggingHook()));

  public void addHook(Function<Path, Boolean> hook) {
    hooks.add(hook);
  }

  private Function<Path, Boolean> loggingHook() {
    return new Function<Path, Boolean>() {
      @Nullable
      @Override
      public Boolean apply(@Nullable Path path) {
        LOG.info("Finished processing the view definition for" + path);
        return true;
      }
    };
  }

  @Override
  public void start() {

    try {
      Path path = buildWatchService();
      Runnable task = startWatching(path);
      watchTask = executorService.submit(task);
    } catch (Exception e) {
      LOG.error("There were errors in starting the view directory watcher. This task will not run", e);
    }
  }


  @SuppressWarnings("unchecked")
  private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  private Runnable startWatching(final Path path) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          while (true) {
            // wait for key , park the thread meanwhile
            WatchKey key = watchService.take();
            LOG.info("Watcher Key was signalled");
            for (WatchEvent<?> event : key.pollEvents()) {
              LOG.info("Watcher recieved poll event");
              WatchEvent<Path> ev = cast(event);
              Path resolvedPath = path.resolve(ev.context());
              LOG.info(String.format("Event %s: %s\n", ev.kind(), resolvedPath));
              if (!canBlockTillFileAvailable(resolvedPath)) {
                LOG.info("Watcher detected that the file was either empty or corrupt");
                continue;
              }
              if (!verify(resolvedPath)) {
                LOG.info("The uploaded file was 1> Empty 2> Not a regular file or 3> Not a valid Jar archive file");
                continue;
              }
              try {
                LOG.info("Starting view extraction");
                viewRegistry.readViewArchive(resolvedPath);
                // fire registered hooks
                for (Function<Path, Boolean> hook : hooks) {
                  hook.apply(resolvedPath);
                }
              } catch (Exception e) {
                LOG.error("Cannot read the view archive, offending file: " + resolvedPath, e);
              }

            }

            // reset key
            if (!key.reset()) {
              //watch key is invalid, break out
              LOG.error("The watch key could not be reset, Directory watcher will not run anymore");
              break;
            }


          }
        } catch (InterruptedException x) {
          LOG.info("Cancelling the directory watcher", x);
          return;
        }

      }
    };
  }


  /**
   * Routine to make the file watcher block the thread till the file is completely copied
   * Check the length of the file continuously till there are 20 consecutive intervals when
   * the file length does not change
   * FILE_CHECK_INTERVAL_MILLIS defines the check interval both for detecting empty files
   * and subsequent checks to detect if a file has finished copying
   *
   * The process which copies the jar into the views dir is external and we dont really
   * know when it would finish, this is also highly OS and FS dependent. The following routine
   * introduces a heuristic to detect when a file has finished copying by looking at subsequent
   * lengths of the file which was detected as being created
   *
   * This would block for ~ 7 seconds in most cases
   *
   *
   * @param resolvedPath
   * @return false if the file check failed, true otherwise
   */
  private boolean canBlockTillFileAvailable(Path resolvedPath) throws InterruptedException {
    long oldLength;
    long newSize;

    long emptyCheck = 0;
    int fixed = 0;
    // get the underlying file
    File file = resolvedPath.toAbsolutePath().toFile();

    // empty file check
    while (file.length() == 0 && emptyCheck < 5) {
      sleep(FILE_CHECK_INTERVAL_MILLIS);
      emptyCheck++;
    }
    // The file seems to be empty
    if (emptyCheck == 5)
      return false;

    // check the file size
    oldLength = file.length();

    // Check if file copy is done
    while (true) {
      LOG.info("Waiting for file to be completely copied");
      sleep(FILE_CHECK_INTERVAL_MILLIS);
      newSize = file.length();
      if (newSize > oldLength) {
        oldLength = newSize;
        continue;
      } else if (oldLength == newSize) {
        fixed++;
      } else {
        // this can never happen,
        return false;
      }
      if (fixed > FIXED_FILE_COUNTER) {
        LOG.info("File " + resolvedPath + " has finished copying");
        return true;
      }
    }


  }

  /**
   * Sanity check to validate if the detected path is a valid archive file
   * is not a directory, also check that the file is not empty
   *
   * @param resolvedPath
   * @return
   */
  private boolean verify(Path resolvedPath) {
    ZipFile zipFile = null;
    try {
      File file = resolvedPath.toAbsolutePath().toFile();
      checkArgument(!file.isDirectory());
      checkArgument(file.length() > 0);
      zipFile = new ZipFile(file);
    } catch (Exception e) {
      LOG.info("Verification failed ", e);
      return false;
    } finally {
      Closeables.closeSilently(zipFile);
    }
    return true;
  }

  private Path buildWatchService() throws IOException {
    // Get the directory for view Archives
    //Attach a file watcher at this directory, Extracted work directory will be ignored
    File viewsDir = configuration.getViewsDir();
    Path path = Paths.get(viewsDir.getAbsolutePath());

    watchService = path.getFileSystem().newWatchService();
    //Watch vews directory for creation events
    path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    return path;

  }

  @Override
  public boolean isRunning() {
    if (watchTask != null)
      return !(watchTask.isDone());
    return false;
  }

  @Override
  public void stop() {
    watchTask.cancel(true);
  }

}
