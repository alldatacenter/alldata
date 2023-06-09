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
package org.apache.drill.exec.vector.accessor.impl;

/**
 * Interface for dumping object state in a hierarchical fashion during
 * debugging.
 */

public interface HierarchicalFormatter {

  HierarchicalFormatter startObject(Object obj);
  void extend();
  HierarchicalFormatter attribute(String label);
  HierarchicalFormatter attribute(String label, Object value);
  HierarchicalFormatter attributeIdentity(String label, Object value);
  HierarchicalFormatter attributeArray(String label);
  HierarchicalFormatter element(int index, Object value);
  HierarchicalFormatter elementIdentity(int index, Object value);
  HierarchicalFormatter element(int index);
  HierarchicalFormatter endArray();
  HierarchicalFormatter endObject();
}
