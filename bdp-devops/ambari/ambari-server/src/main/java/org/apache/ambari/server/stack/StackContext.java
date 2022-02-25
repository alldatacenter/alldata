/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.stack;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.stack.RepoUrlInfoCallable;
import org.apache.ambari.server.state.stack.RepoUrlInfoCallable.RepoUrlInfoResult;
import org.apache.ambari.server.state.stack.RepoVdfCallable;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides external functionality to the Stack framework.
 */
public class StackContext {
  /**
   * Metainfo data access object
   */
  private MetainfoDAO metaInfoDAO;

  /**
   * Action meta data functionality
   */
  private ActionMetadata actionMetaData;

  /**
   * Executor used to get latest repo url's
   */
  private LatestRepoQueryExecutor repoUpdateExecutor;

  private final static Logger LOG = LoggerFactory.getLogger(StackContext.class);
  private static final int THREAD_COUNT = 10;


  /**
   * Constructor.
   *
   * @param metaInfoDAO     metainfo data access object
   * @param actionMetaData  action meta data
   * @param osFamily        OS family information
   */
  public StackContext(MetainfoDAO metaInfoDAO, ActionMetadata actionMetaData, OsFamily osFamily) {
    this.metaInfoDAO = metaInfoDAO;
    this.actionMetaData = actionMetaData;
    repoUpdateExecutor = new LatestRepoQueryExecutor(osFamily);
  }

  /**
   * Register a service check.
   *
   * @param serviceName  name of the service
   */
  public void registerServiceCheck(String serviceName) {
    actionMetaData.addServiceCheckAction(serviceName);
  }

  /**
   * Register a task to obtain the latest repo url from an external location.
   *
   * @param url    external repo information URL
   * @param stack  stack module
   */
  public void registerRepoUpdateTask(URI uri, StackModule stack) {
    repoUpdateExecutor.addTask(uri, stack);
  }

  /**
   * Execute the registered repo update tasks.
   */
  public void executeRepoTasks() {
    repoUpdateExecutor.execute();
  }

  /**
   * Determine if all registered repo update tasks have completed.
   *
   * @return true if all tasks have completed; false otherwise
   */
  public boolean haveAllRepoTasksCompleted() {
    return repoUpdateExecutor.hasCompleted();
  }


  /**
   * Executor used to execute repository update tasks.
   * Tasks will be executed in a single executor thread.
   */
  public static class LatestRepoQueryExecutor {
    /**
     * Registered tasks
     */
    private Map<URI, RepoUrlInfoCallable> tasks = new HashMap<>();

    /**
     * Task futures
     */
    Collection<Future<?>> futures = new ArrayList<>();
    /**
     * Underlying executor
     */
    private ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Stack Version Loading Thread");
      }
    });


    private OsFamily m_family;

    private LatestRepoQueryExecutor(OsFamily family) {
      m_family = family;
    }

    /**
     * @param uri
     *          uri to load
     * @param stackModule
     *          the stack module
     */
    public void addTask(URI uri, StackModule stackModule) {
      RepoUrlInfoCallable callable = null;
      if (tasks.containsKey(uri)) {
        callable = tasks.get(uri);
      } else {
        callable = new RepoUrlInfoCallable(uri);
        tasks.put(uri, callable);
      }

      callable.addStack(stackModule);
    }

    /**
     * Execute all tasks.
     */
    public void execute() {

      long currentTime = System.nanoTime();
      List<Future<Map<StackModule, RepoUrlInfoResult>>> results;

      // !!! first, load the *_urlinfo.json files and block for completion
      try {
        results = executor.invokeAll(tasks.values(), 2, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        LOG.warn("Could not load urlinfo as the executor was interrupted", e);
        return;
      } finally {
        LOG.info("Loaded urlinfo in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentTime) + "ms");
      }

      List<Map<StackModule, RepoUrlInfoResult>> urlInfoResults = new ArrayList<>();
      // !!! now load all the VDF _by version_ in a new thread.
      for (Future<Map<StackModule, RepoUrlInfoResult>> future : results) {
        try {
          urlInfoResults.add(future.get());
        } catch (Exception e) {
          LOG.error("Could not load repo results", e.getCause());
        }
      }

      currentTime = System.nanoTime();
      for (Map<StackModule, RepoUrlInfoResult> urlInfoResult : urlInfoResults) {
        for (Entry<StackModule, RepoUrlInfoResult> entry : urlInfoResult.entrySet()) {
          StackModule stackModule = entry.getKey();
          RepoUrlInfoResult result = entry.getValue();

          if (null != result) {
            if (MapUtils.isNotEmpty(result.getManifest())) {
              for (Entry<String, Map<String, URI>> manifestEntry : result.getManifest().entrySet()) {
                futures.add(executor.submit(new RepoVdfCallable(stackModule, manifestEntry.getKey(),
                    manifestEntry.getValue(), m_family)));
              }
            }

            if (MapUtils.isNotEmpty(result.getLatestVdf())) {
             futures.add(executor.submit(
                 new RepoVdfCallable(stackModule, result.getLatestVdf(), m_family)));
            }
          }
        }
      }

      executor.shutdown();

      try {
        executor.awaitTermination(2,  TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        LOG.warn("Loading all VDF was interrupted", e.getCause());
      } finally {
        LOG.info("Loaded all VDF in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - currentTime) + "ms");
      }
    }

    /**
     * Determine whether all tasks have completed.
     *
     * @return true if all tasks have completed; false otherwise
     */
    public boolean hasCompleted() {
      for (Future<?> f : futures) {
        if (! f.isDone()) {
          return false;
        }
      }
      return true;
    }
  }
}
