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
package org.apache.drill.exec.planner.fragment;

import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import java.util.Collection;

/**
 * Generic interface to provide different parallelization strategies for MajorFragments.
 */
public interface FragmentParallelizer {
  /**
   * Parallelize the given fragment.
   *
   * @param fragment
   * @param parameters
   * @param activeEndpoints
   * @throws PhysicalOperatorSetupException
   */
  void parallelizeFragment(final Wrapper fragment, final ParallelizationParameters parameters,
      final Collection<DrillbitEndpoint> activeEndpoints) throws PhysicalOperatorSetupException;
}
