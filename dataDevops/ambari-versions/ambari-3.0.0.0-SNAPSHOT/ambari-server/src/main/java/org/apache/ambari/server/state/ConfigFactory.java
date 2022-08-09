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
package org.apache.ambari.server.state;

import java.util.Map;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;

import com.google.inject.assistedinject.Assisted;

/**
 * Factory for creating configuration objects using {@link Assisted} constructor parameters
 */
public interface ConfigFactory {

  /**
   * Creates a new {@link Config} object using provided values.
   */
  @Experimental(feature = ExperimentalFeature.MULTI_SERVICE,
      comment = "This constructor is only used for test compatibility and should be removed")
  Config createNew(Cluster cluster, @Assisted("type") String type, @Assisted("tag") String tag,
      Map<String, String> map, Map<String, Map<String, String>> mapAttributes);


  /**
   * Creates a new {@link Config} object using provided values.
   *
   * @param cluster
   * @param type
   * @param tag
   * @param map
   * @param mapAttributes
   * @return
   */
  Config createNew(StackId stackId, Cluster cluster, @Assisted("type") String type, @Assisted("tag") String tag,
      Map<String, String> map, Map<String, Map<String, String>> mapAttributes);

  /**
   * Creates a new {@link Config} object using provided entity
   *
   * @param cluster
   * @param entity
   * @return
   */
  Config createExisting(Cluster cluster, ClusterConfigEntity entity);

  /**
   * Creates a read-only instance of a {@link Config} suitable for returning in
   * REST responses.
   *
   * @param type
   * @param tag
   * @param map
   * @param mapAttributes
   * @return
   */
  Config createReadOnly(@Assisted("type") String type, @Assisted("tag") String tag,
      Map<String, String> map, Map<String, Map<String, String>> mapAttributes);
}
