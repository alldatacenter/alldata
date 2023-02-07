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
package org.apache.drill.common.scanner;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * main class to integrate classpath scanning in the build.
 * @see BuildTimeScan#main(String[])
 */
public class BuildTimeScan {
  private static final Logger logger = LoggerFactory.getLogger(BuildTimeScan.class);
  private static final String REGISTRY_FILE = "META-INF/drill-module-scan/registry.json";

  private static final ObjectMapper mapper = new ObjectMapper().enable(INDENT_OUTPUT);
  private static final ObjectReader reader = mapper.readerFor(ScanResult.class);
  private static final ObjectWriter writer = mapper.writerFor(ScanResult.class);

  /**
   * @return paths that have the prescanned registry file in them
   */
  static Set<URL> getPrescannedPaths() {
    return ClassPathScanner.forResource(REGISTRY_FILE, true);
  }

  /**
   * loads all the prescanned resources from classpath
   * @return the result of the previous scan
   */
  static ScanResult load() {
    return loadExcept(null);
  }

  /**
   * loads all the prescanned resources from classpath
   * (except for the target location in case it already exists)
   * @return the result of the previous scan
   */
  private static ScanResult loadExcept(URL ignored) {
    Set<URL> preScanned = ClassPathScanner.forResource(REGISTRY_FILE, false);
    ScanResult result = null;
    for (URL u : preScanned) {
      if (ignored!= null && u.toString().startsWith(ignored.toString())) {
        continue;
      }
      try (InputStream reflections = u.openStream()) {
        ScanResult ref = reader.readValue(reflections);
        if (result == null) {
          result = ref;
        } else {
          result = result.merge(ref);
        }
      } catch (IOException e) {
        throw new DrillRuntimeException("can't read function registry at " + u, e);
      }
    }
    if (result != null) {
      if (logger.isInfoEnabled()) {
        StringBuilder sb = new StringBuilder();
        sb.append(format("Loaded prescanned packages %s from locations:\n", result.getScannedPackages()));
        for (URL u : preScanned) {
          sb.append('\t');
          sb.append(u.toExternalForm());
          sb.append('\n');
        }
      }
      logger.info(format("Loaded prescanned packages %s from locations %s", result.getScannedPackages(), preScanned));
      return result;
    } else {
      return ClassPathScanner.emptyResult();
    }
  }

  private static void save(ScanResult scanResult, File file) {
    try {
      writer.writeValue(file, scanResult);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * to generate the prescan file during build
   * @param args the root path for the classes where {@link BuildTimeScan#REGISTRY_FILE} is generated
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: java {cp} " + BuildTimeScan.class.getName() + " path/to/scan");
    }
    String basePath = args[0];
    logger.info("Scanning: {}", basePath);
    File registryFile = new File(basePath, REGISTRY_FILE);
    File dir = registryFile.getParentFile();
    if ((!dir.exists() && !dir.mkdirs()) || !dir.isDirectory()) {
      throw new IllegalArgumentException("could not create dir " + dir.getAbsolutePath());
    }
    DrillConfig config = DrillConfig.create();
    // normalize
    if (!basePath.endsWith("/")) {
      basePath = basePath + "/";
    }
    if (!basePath.startsWith("/")) {
      basePath = "/" + basePath;
    }
    URL url = new URL("file:" + basePath);
    Set<URL> markedPaths = ClassPathScanner.getMarkedPaths(ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME);
    if (!markedPaths.contains(url)) {
      throw new IllegalArgumentException(url + " not in " + markedPaths);
    }
    List<String> packagePrefixes = ClassPathScanner.getPackagePrefixes(config);
    List<String> baseClasses = ClassPathScanner.getScannedBaseClasses(config);
    List<String> scannedAnnotations = ClassPathScanner.getScannedAnnotations(config);
    ScanResult preScanned = loadExcept(url);
    ScanResult scan = ClassPathScanner.scan(
        asList(url),
        packagePrefixes,
        baseClasses,
        scannedAnnotations,
        preScanned);
    save(scan, registryFile);
  }
}
