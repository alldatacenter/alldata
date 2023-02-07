/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common.util;

import java.lang.reflect.Modifier;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.scopedpool.ScopedClassPoolRepository;
import javassist.scopedpool.ScopedClassPoolRepositoryImpl;

public class GuavaPatcher {
  private static final Logger logger = LoggerFactory.getLogger(GuavaPatcher.class);

  private static boolean patchingAttempted;

  public static synchronized void patch() {
    if (!patchingAttempted) {
      patchingAttempted = true;
      patchStopwatch();
      patchCloseables();
      patchFuturesCallback();
    }
  }

  private static boolean patchingSuccessful = true;

  @VisibleForTesting
  static boolean isPatchingSuccessful() {
    return patchingAttempted && patchingSuccessful;
  }

  /**
   * Makes Guava stopwatch look like the old version for compatibility with hbase-server (for test purposes).
   */
  private static void patchStopwatch() {
    try {
      ClassPool cp = getClassPool();
      CtClass cc = cp.get("com.google.common.base.Stopwatch");

      // Expose the constructor for Stopwatch for old libraries who use the pattern new Stopwatch().start().
      for (CtConstructor c : cc.getConstructors()) {
        if (!Modifier.isStatic(c.getModifiers())) {
          c.setModifiers(Modifier.PUBLIC);
        }
      }

      // Add back the Stopwatch.elapsedMillis() method for old consumers.
      CtMethod newMethod = CtNewMethod.make(
          "public long elapsedMillis() { return elapsed(java.util.concurrent.TimeUnit.MILLISECONDS); }", cc);
      cc.addMethod(newMethod);

      // Load the modified class instead of the original.
      cc.toClass();

      logger.info("Google's Stopwatch patched for old HBase Guava version.");
    } catch (Exception e) {
      logUnableToPatchException(e);
    }
  }

  private static void logUnableToPatchException(Exception e) {
    patchingSuccessful = false;
    logger.warn("Unable to patch Guava classes: {}", e.getMessage());
    logger.debug("Exception:", e);
  }

  private static void patchCloseables() {
    try {
      ClassPool cp = getClassPool();
      CtClass cc = cp.get("com.google.common.io.Closeables");

      // Add back the Closeables.closeQuietly() method for old consumers.
      CtMethod newMethod = CtNewMethod.make(
          "public static void closeQuietly(java.io.Closeable closeable) { try{closeable.close();}catch(Exception e){} }",
          cc);
      cc.addMethod(newMethod);

      // Load the modified class instead of the original.
      cc.toClass();

      logger.info("Google's Closeables patched for old HBase Guava version.");
    } catch (Exception e) {
      logUnableToPatchException(e);
    }
  }

  /**
   * Returns {@link javassist.scopedpool.ScopedClassPool} instance which uses the same class loader
   * which was used for loading current class.
   *
   * @return {@link javassist.scopedpool.ScopedClassPool} instance
   */
  private static ClassPool getClassPool() {
    ScopedClassPoolRepository classPoolRepository = ScopedClassPoolRepositoryImpl.getInstance();
    // sets prune flag to false to avoid freezing and pruning classes right after obtaining CtClass instance
    classPoolRepository.setPrune(false);
    return classPoolRepository.createScopedClassPool(GuavaPatcher.class.getClassLoader(), null);
  }

  /**
   * Makes Guava Futures#addCallback look like the old version for compatibility with hadoop-hdfs (for test purposes).
   */
  private static void patchFuturesCallback() {
    try {
      ClassPool cp = getClassPool();
      CtClass cc = cp.get("com.google.common.util.concurrent.Futures");

      // Add back the Futures.addCallback(ListenableFuture future, FutureCallback callback) method for old consumers.
      CtMethod newMethod = CtNewMethod.make(
        "public static void addCallback(" +
                "com.google.common.util.concurrent.ListenableFuture future, " +
                "com.google.common.util.concurrent.FutureCallback callback" +
          ") { addCallback(future, callback, com.google.common.util.concurrent.MoreExecutors.directExecutor()); }", cc);
      cc.addMethod(newMethod);

      // Load the modified class instead of the original.
      cc.toClass();
      logger.info("Google's Futures#addCallback patched for old Hadoop Guava version.");
    } catch (Exception e) {
      logUnableToPatchException(e);
    }
  }
}
