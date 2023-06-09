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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.exec.compile.TemplateClassDefinition;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.VectorContainer;


public interface JoinWorker {
  TemplateClassDefinition<JoinWorker> TEMPLATE_DEFINITION = new TemplateClassDefinition<>(JoinWorker.class, JoinTemplate.class);

  enum JoinOutcome {
    NO_MORE_DATA, BATCH_RETURNED, SCHEMA_CHANGED, WAITING, FAILURE;
  }

  void setupJoin(FragmentContext context, JoinStatus status, VectorContainer outgoing) throws SchemaChangeException;
  boolean doJoin(JoinStatus status);
}
