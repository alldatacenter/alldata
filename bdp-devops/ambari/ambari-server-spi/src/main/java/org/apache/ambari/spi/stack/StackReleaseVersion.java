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
package org.apache.ambari.spi.stack;

import java.util.Comparator;

/**
 * A stack supplies an implementation class to handle version representations.
 */
public interface StackReleaseVersion {

  /**
   * @param info
   *          the release info instance
   * @return  the full display string
   */
  String getFullVersion(StackReleaseInfo info);

  /**
   * @return a comparator of release info
   */
  Comparator<StackReleaseInfo> getComparator();

  /**
   * Parses a version string to a stack release info
   * @param versionString
   *          the version string
   * @return the corresponding stack release.  Never {@code null}.
   */
  StackReleaseInfo parse(String versionString);
}

