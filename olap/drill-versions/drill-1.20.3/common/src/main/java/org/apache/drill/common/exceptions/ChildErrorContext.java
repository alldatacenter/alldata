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
package org.apache.drill.common.exceptions;

/**
 * Represents an additional level of error context detail that
 * adds to that provided by some outer context. Done via composition
 * rather than subclassing to allow many child contexts to work with
 * many parent contexts.
 */

public class ChildErrorContext implements CustomErrorContext {

  private final CustomErrorContext parent;

  public ChildErrorContext(CustomErrorContext parent) {
    this.parent = parent;
  }

  @Override
  public void addContext(UserException.Builder builder) {
    if (parent != null) {
      parent.addContext(builder);
    }
  }
}
