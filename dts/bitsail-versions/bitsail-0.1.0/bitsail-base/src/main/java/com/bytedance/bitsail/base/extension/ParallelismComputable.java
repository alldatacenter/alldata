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

package com.bytedance.bitsail.base.extension;

import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;

import java.io.Serializable;

public interface ParallelismComputable extends Serializable {

  /**
   * give a parallelism advice for reader/writer based on configurations and upstream parallelism advice
   *
   * @param commonConf     common configuration
   * @param selfConf       reader/writer configuration
   * @param upstreamAdvice parallelism advice from upstream (when an operator has no upstream in DAG, its upstream is
   *                       global parallelism)
   * @return parallelism advice for the reader/writer
   */
  ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                         BitSailConfiguration selfConf,
                                         ParallelismAdvice upstreamAdvice) throws Exception;
}
