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
package org.apache.drill.exec.coord.store;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.exec.serialization.InstanceSerializer;

public class TransientStoreConfigBuilder<T> {
  private String name;
  private InstanceSerializer<T> serializer;

  protected TransientStoreConfigBuilder() { }

  public String name() {
    return name;
  }

  public TransientStoreConfigBuilder<T> name(final String name) {
    this.name = Preconditions.checkNotNull(name);
    return this;
  }

  public InstanceSerializer<T> serializer() {
    return serializer;
  }

  public TransientStoreConfigBuilder<T> serializer(final InstanceSerializer<T> serializer) {
    this.serializer = Preconditions.checkNotNull(serializer);
    return this;
  }

  public TransientStoreConfig<T> build() {
    return new TransientStoreConfig<>(name, serializer);
  }
}
