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

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.persistence.ScanResult;

/**
 * Utility to scan classpath at runtime
 */
public class RunTimeScan {

  /** Result of prescan */
  private static final ScanResult PRESCANNED = BuildTimeScan.load();

  /** URLs of the locations (classes directory or jar) to scan that don't have a registry in them */
  private static final Collection<URL> NON_PRESCANNED_MARKED_PATHS = getNonPrescannedMarkedPaths();

  /**
   * @return getMarkedPaths() sans getPrescannedPaths()
   */
  static Collection<URL> getNonPrescannedMarkedPaths() {
    Collection<URL> markedPaths = ClassPathScanner.getMarkedPaths(ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME);
    markedPaths.removeAll(BuildTimeScan.getPrescannedPaths());
    return markedPaths;
  }

  /**
   * Loads prescanned classpath info and scans for extra ones based on configuration.
   * (unless prescan is disabled with {@link ClassPathScanner#IMPLEMENTATIONS_SCAN_CACHE}{@code =false})
   * @param config to retrieve the packages to scan
   * @return the scan result
   */
  public static ScanResult fromPrescan(DrillConfig config) {
    List<String> packagePrefixes = ClassPathScanner.getPackagePrefixes(config);
    List<String> scannedBaseClasses = ClassPathScanner.getScannedBaseClasses(config);
    List<String> scannedAnnotations = ClassPathScanner.getScannedAnnotations(config);
    if (ClassPathScanner.isScanBuildTimeCacheEnabled(config)) {
      // scan only locations that have not been scanned yet
      ScanResult runtimeScan = ClassPathScanner.scan(
          NON_PRESCANNED_MARKED_PATHS,
          packagePrefixes,
          scannedBaseClasses,
          scannedAnnotations,
          PRESCANNED);
      return runtimeScan.merge(PRESCANNED);
    } else {
      // scan everything
      return ClassPathScanner.scan(
          ClassPathScanner.getMarkedPaths(ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME),
          packagePrefixes,
          scannedBaseClasses,
          scannedAnnotations,
          ClassPathScanner.emptyResult());
    }
  }

  /**
   * Scans packages retrieved from config.
   * Returns scan result with list of packages, classes and annotations found.
   * Is used to scan specific jars not associated with classpath at runtime.
   *
   * @param config to retrieve the packages to scan
   * @param markedPath list of paths where to scan
   * @return the scan result with list of packages, classes and annotations found
   */
  public static ScanResult dynamicPackageScan(DrillConfig config, Set<URL> markedPath) {
    List<String> packagePrefixes = ClassPathScanner.getPackagePrefixes(config);
    return ClassPathScanner.scan(
        markedPath,
        packagePrefixes,
        Lists.<String>newArrayList(),
        PRESCANNED.getScannedAnnotations(),
        ClassPathScanner.emptyResult());
  }
}
