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
package org.apache.drill.exec.expr.fn.registry;

import org.apache.drill.common.scanner.persistence.ScanResult;

/**
 * Holder class that contains:
 * <ol>
 *   <li>jar name</li>
 *   <li>scan of packages, classes, annotations found in jar</li>
 *   <li>unique jar classLoader</li>
 * </ol>
 */
public class JarScan {

  private final String jarName;
  private final ScanResult scanResult;
  private final ClassLoader classLoader;

  public JarScan(String jarName, ScanResult scanResult, ClassLoader classLoader) {
    this.jarName = jarName;
    this.scanResult = scanResult;
    this.classLoader = classLoader;
  }

  public String getJarName() {
    return jarName;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public ScanResult getScanResult() {
    return scanResult;
  }
}
