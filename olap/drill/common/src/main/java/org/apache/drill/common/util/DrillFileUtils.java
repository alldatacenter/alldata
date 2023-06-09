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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

public class DrillFileUtils {
  // These two seperators are intentionally used instead of Path.SEPERATPR or File.seperator
  // for loading classes since both alternatives return '\' on Windows. However, in the context
  // of classpath scanning and loading only '/' is valid.
  public static final char SEPARATOR_CHAR = '/';
  public static final String SEPARATOR = "/";

  public static File getResourceAsFile(String fileName) throws IOException {
    URL u = DrillFileUtils.class.getResource(fileName);
    if (u == null) {
      throw new FileNotFoundException(String.format("Unable to find file on path %s", fileName));
    }
    return new File(u.getPath());
  }

  public static String getResourceAsString(String fileName) throws IOException {
    return Files.asCharSource(getResourceAsFile(fileName), Charsets.UTF_8).read();
  }

  /**
   * Creates a temporary directory under the default temporary directory location.
   * This is a safe replacement for Guava {@code Files#createTempDir()}
   *
   * @return a temporary directory
   * @throws IllegalStateException if the directory cannot be created
   */
  public static File createTempDir() {
    try {
      return java.nio.file.Files.createTempDirectory(System.currentTimeMillis() + "-").toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temporary directory");
    }
  }
}
