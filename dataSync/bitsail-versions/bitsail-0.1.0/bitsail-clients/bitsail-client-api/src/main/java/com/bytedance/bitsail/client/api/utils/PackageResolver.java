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

package com.bytedance.bitsail.client.api.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created 2022/8/10
 */
public class PackageResolver {

  static String BITSAIL_HOME_KEY = "BITSAIL_HOME";

  private static String LIBS_PATH = "./libs";
  private static String CLIENT_PATH = LIBS_PATH + "/clients";
  private static String COMPONENTS_PATH = LIBS_PATH + "components";
  private static String CONNECTORS_PATH = LIBS_PATH + "connectors";
  private static String EMBEDDED_ENGINE_PATH = "./embedded/";

  public static Path getLibraryDir() {
    return getRootDir().resolve(LIBS_PATH);
  }

  public static Path getClientDir() {
    return getRootDir().resolve(CLIENT_PATH);
  }

  public static Path getEmbeddedEngineDir() {
    return getRootDir().resolve(EMBEDDED_ENGINE_PATH);
  }

  public static Path getConnectorsDir() {
    return getRootDir().resolve(CONNECTORS_PATH);
  }

  public static Path getComponentsDir() {
    return getRootDir().resolve(COMPONENTS_PATH);
  }

  private static Path getRootDir() {
    String envHomeDir = System.getenv(BITSAIL_HOME_KEY);
    if (StringUtils.isNotEmpty(envHomeDir)) {
      return Paths.get(envHomeDir);
    }

    try {
      String path = PackageResolver.class.getProtectionDomain().getCodeSource()
          .getLocation().toURI().getPath();
      path = new File(path).getPath();
      return Paths.get(path).getParent().getParent();
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

}
