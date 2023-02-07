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
package org.apache.drill.exec.physical.base;

import org.apache.drill.exec.physical.impl.join.RowKeyJoin;

import com.fasterxml.jackson.annotation.JsonIgnore;


public interface DbSubScan extends SubScan {

  /**
   * Whether this subscan is a restricted (skip) subscan
   * @return true if this subscan is a restricted subscan, false otherwise
   */
  @JsonIgnore
  boolean isRestrictedSubScan();

  /**
   * For a restricted sub-scan, this method allows associating a rowkey join instance.  A subscan within a minor
   * fragment must have a corresponding rowkey join batch instance from which it will retrieve its set of
   * rowkeys to perform the restricted scan.
   * @param batch
   */
  @JsonIgnore
  void addJoinForRestrictedSubScan(RowKeyJoin batch);

}
