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
package org.apache.ambari.server.stack.upgrade;

import javax.xml.bind.annotation.XmlElement;

/**
 *  Identifies if component instances should be upgraded in parallel (optional)
 */
public class ParallelScheduler {

  // This setting can be overridden using ambari.properties file
  public static final int DEFAULT_MAX_DEGREE_OF_PARALLELISM = Integer.MAX_VALUE;

  @XmlElement(name="max-degree-of-parallelism")
  public int maxDegreeOfParallelism = DEFAULT_MAX_DEGREE_OF_PARALLELISM;
}
