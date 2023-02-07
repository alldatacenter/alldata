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
package org.apache.drill.exec.cache;

import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.server.options.OptionValue;

public enum SerializationDefinition {

  OPTION(3002, OptionValue.class),
  STORAGE_PLUGINS(3003, StoragePlugins.class),
  FRAGMENT_STATUS(3004, FragmentStatus.class),
  FRAGMENT_HANDLE(3005, FragmentHandle.class),
  PLAN_FRAGMENT(3006, PlanFragment.class);

  public final int id;
  public final Class<?> clazz;

  SerializationDefinition(int id, Class<?> clazz){
    this.id = id;
    this.clazz = clazz;
  }

}
