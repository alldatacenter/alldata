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
package org.apache.drill.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.drill.common.util.RepeatTestRule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

public class TestTools {
  public enum FileSource {
    RESOURCE,
    PROJECT
  }

  public static final Path TEST_RESOURCES_REL = Paths.get("src", "test", "resources");
  public static final Path PROJECT_ROOT = Paths.get("..", "..");
  public static final Path WORKING_PATH = new File(".").toPath();

  public static final Path TEST_RESOURCES_ABS = WORKING_PATH.resolve(TEST_RESOURCES_REL);
  public static final Path SAMPLE_DATA = PROJECT_ROOT.resolve("sample-data");

  static final boolean IS_DEBUG = java.lang.management.ManagementFactory.getRuntimeMXBean()
    .getInputArguments()
    .toString()
    .indexOf("-agentlib:jdwp") > 0;

  public static TestRule getTimeoutRule(int timeout) {
    return IS_DEBUG ? new TestName() : Timeout.millis(timeout);
  }

  /**
   * If not enforced, the repeat rule applies only if the test is run in non-debug mode.
   */
  public static TestRule getRepeatRule(final boolean enforce) {
    return enforce || !IS_DEBUG ? new RepeatTestRule() : new TestName();
  }

  public static File getResourceFile(Path relPath) {
    return WORKING_PATH
      .resolve(TEST_RESOURCES_REL)
      .resolve(relPath)
      .toFile();
  }

  public static File getProjectFile(Path relPath) {
    return WORKING_PATH
      .resolve(PROJECT_ROOT)
      .resolve(relPath)
      .toFile();
  }

  public static File getFile(Path relPath, FileSource fileSource) {
    switch (fileSource) {
      case RESOURCE:
        return getResourceFile(relPath);
      case PROJECT:
        return getProjectFile(relPath);
      default: {
        throw new IllegalArgumentException(String.format("Unkown data type %s", fileSource));
      }
    }
  }

  public static void copyDirToDest(Path relPath, File destDir, FileSource fileSource) {
    File srcDir;

    switch (fileSource) {
      case RESOURCE:
        srcDir = getResourceFile(relPath);
        break;
      case PROJECT:
        srcDir = getProjectFile(relPath);
        break;
      default: {
        throw new IllegalArgumentException(String.format("Unkown data type %s", fileSource));
      }
    }

    try {
      FileUtils.copyDirectory(srcDir, destDir);
    } catch (IOException e) {
      throw new RuntimeException("This should never happen", e);
    }
  }
}
