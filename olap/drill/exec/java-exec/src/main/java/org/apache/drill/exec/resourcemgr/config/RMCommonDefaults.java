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
package org.apache.drill.exec.resourcemgr.config;

import org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy.SelectionPolicy;

/**
 * Defines all the default values used for the optional configurations for ResourceManagement
 */
public final class RMCommonDefaults {

  public static final int MAX_ADMISSIBLE_QUERY_COUNT = 10;

  public static final int MAX_WAITING_QUERY_COUNT = 10;

  public static final int MAX_WAIT_TIMEOUT_IN_MS = 30_000;

  public static final boolean WAIT_FOR_PREFERRED_NODES = true;

  public static final double ROOT_POOL_DEFAULT_MEMORY_PERCENT = 0.9;

  public static final SelectionPolicy ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY = SelectionPolicy.BESTFIT;

}
