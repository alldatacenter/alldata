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

package com.netease.arctic.optimizer.factory;

import com.netease.arctic.ams.api.properties.OptimizerProperties;
import com.netease.arctic.optimizer.Optimizer;
import com.netease.arctic.optimizer.util.DefaultOptimizerSerializer;

import java.util.Map;

public interface OptimizerFactory {

  /**
   * One container type(eg:flink,local,spark) just need one OptimizerFactory, so there should return the
   * container type name which container this OptimizerFactory belong.
   *
   * @return -
   */
  String identify();

  /**
   * Create Optimizer with Optimize properties, common properties are {@link OptimizerProperties}.
   *
   * @param name       -
   * @param properties -
   * @return -
   */
  Optimizer createOptimizer(String name, Map<String, String> properties);

  default Optimizer deserialize(byte[] bytes) {
    return DefaultOptimizerSerializer.deserialize(bytes);
  }

  default byte[] serialize(Optimizer optimizer) {
    return DefaultOptimizerSerializer.serialize(optimizer);
  }

}
