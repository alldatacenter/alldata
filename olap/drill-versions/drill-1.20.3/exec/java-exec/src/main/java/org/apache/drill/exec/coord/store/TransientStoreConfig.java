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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.protostuff.Schema;
import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import com.google.protobuf.Message;
import org.apache.drill.exec.serialization.JacksonSerializer;
import org.apache.drill.exec.serialization.ProtoSerializer;
import org.apache.drill.exec.serialization.InstanceSerializer;

public class TransientStoreConfig<V> {
  private final String name;
  private final InstanceSerializer<V> serializer;

  protected TransientStoreConfig(final String name, final InstanceSerializer<V> serializer) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name is required");
    this.name = name;
    this.serializer = Preconditions.checkNotNull(serializer, "serializer cannot be null");
  }

  public String getName() {
    return name;
  }

  public InstanceSerializer<V> getSerializer() {
    return serializer;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, serializer);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TransientStoreConfig && obj.getClass().equals(getClass())) {
      @SuppressWarnings("unchecked")
      final TransientStoreConfig<V> other = (TransientStoreConfig<V>)obj;
      return Objects.equal(name, other.name) && Objects.equal(serializer, other.serializer);
    }
    return false;
  }

  public static <V> TransientStoreConfigBuilder<V> newBuilder() {
    return new TransientStoreConfigBuilder<>();
  }

  public static <V extends Message, B extends Message.Builder> TransientStoreConfigBuilder<V> newProtoBuilder(final Schema<V> writeSchema, final Schema<B> readSchema) {
    return TransientStoreConfig.<V>newBuilder().serializer(new ProtoSerializer<>(readSchema, writeSchema));
  }

  public static <V> TransientStoreConfigBuilder<V> newJacksonBuilder(final ObjectMapper mapper, final Class<V> klazz) {
    return TransientStoreConfig.<V>newBuilder().serializer(new JacksonSerializer<>(mapper, klazz));
  }
}
