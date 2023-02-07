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
package org.apache.drill.exec.planner.logical;

public interface DrillTableSelection {

  /**
   * The digest of the selection represented by the implementation. The
   * selections that accompany Tables can modify the contained dataset, e.g.
   * a file selection can restrict to a subset of the available data and a
   * format selection can include options that affect the behaviour of the
   * underlying reader. Two scans will end up being considered identical during
   * logical planning if their digests are the same so selection
   * implementations should override this method so that exactly those scans
   * that really are identical (in terms of the data they produce) have matching
   * digests.
   *
   * @return this selection's digest, normally a string built from its properties.
   */
  public String digest();
}
