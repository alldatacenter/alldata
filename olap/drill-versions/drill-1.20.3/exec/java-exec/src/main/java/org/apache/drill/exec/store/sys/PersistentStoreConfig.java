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
package org.apache.drill.exec.store.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import io.protostuff.Schema;
import org.apache.drill.exec.serialization.InstanceSerializer;
import org.apache.drill.exec.serialization.JacksonSerializer;
import org.apache.drill.exec.serialization.ProtoSerializer;


/**
 * An abstraction for configurations that are used to create a {@link PersistentStore store}.
 *
 * @param <V>  value type of which {@link PersistentStore} uses to store & retrieve instances
 */
public class PersistentStoreConfig<V> {

  private final String name;
  private final InstanceSerializer<V> valueSerializer;
  private final PersistentStoreMode mode;
  private final int capacity;

  protected PersistentStoreConfig(String name, InstanceSerializer<V> valueSerializer, PersistentStoreMode mode, int capacity) {
    this.name = name;
    this.valueSerializer = valueSerializer;
    this.mode = mode;
    this.capacity = capacity;
  }

  public int getCapacity() {
    return capacity;
  }

  public PersistentStoreMode getMode() {
    return mode;
  }

  public String getName() {
    return name;
  }

  public InstanceSerializer<V> getSerializer() {
    return valueSerializer;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, valueSerializer, mode);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PersistentStoreConfig) {
      final PersistentStoreConfig<?> other = PersistentStoreConfig.class.cast(obj);
      return Objects.equal(name, other.name)
          && Objects.equal(valueSerializer, other.valueSerializer)
          && Objects.equal(mode, other.mode);
    }
    return false;
  }

  public static <V extends Message, X extends Builder> StoreConfigBuilder<V> newProtoBuilder(Schema<V> writeSchema, Schema<X> readSchema) {
    return new StoreConfigBuilder<>(new ProtoSerializer<>(readSchema, writeSchema));
  }

  public static <V> StoreConfigBuilder<V> newJacksonBuilder(ObjectMapper mapper, Class<V> clazz) {
    return new StoreConfigBuilder<>(new JacksonSerializer<>(mapper, clazz));
  }

  public static class StoreConfigBuilder<V> {
    private String name;
    private InstanceSerializer<V> serializer;
    private PersistentStoreMode mode = PersistentStoreMode.PERSISTENT;
    private int capacity;

    protected StoreConfigBuilder(InstanceSerializer<V> serializer) {
      super();
      this.serializer = serializer;
    }

    public StoreConfigBuilder<V> name(String name) {
      this.name = name;
      return this;
    }

    public StoreConfigBuilder<V> persist(){
      this.mode = PersistentStoreMode.PERSISTENT;
      return this;
    }

    public StoreConfigBuilder<V> blob(){
      this.mode = PersistentStoreMode.BLOB_PERSISTENT;
      return this;
    }

    public StoreConfigBuilder<V> setCapacity(int capacity) {
      this.capacity = capacity;
      return this;
    }

    public PersistentStoreConfig<V> build(){
      Preconditions.checkNotNull(name);
      return new PersistentStoreConfig<>(name, serializer, mode, capacity);
    }
  }

}
