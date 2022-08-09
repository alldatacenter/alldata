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
package org.apache.ambari.server.controller.spi;

/**
 * The predicate is used to filter the resources returned from the cluster
 * controller.  The predicate can examine a resource object and determine
 * whether or not it should be included in the returned results.
 */
public interface Predicate {
  /**
   * Evaluate the predicate for the given resource.
   *
   * @param resource the resource to evaluate the predicate against
   * @return the result of applying the predicate to the given resource
   */
  boolean evaluate(Resource resource);
}
