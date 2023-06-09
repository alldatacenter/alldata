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

import org.apache.drill.exec.exception.DrillbitStartupException;

import com.google.protobuf.Message;

public interface DistributedCache extends AutoCloseable{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DistributedCache.class);

  public void run() throws DrillbitStartupException;

  public <K, V> DistributedMap<K, V> getMap(CacheConfig<K, V> config);
  public <K, V> DistributedMultiMap<K, V> getMultiMap(CacheConfig<K, V> config);

  public Counter getCounter(String name);

  public static enum SerializationMode {
    JACKSON(Object.class),
    DRILL_SERIALIZIABLE(String.class, DrillSerializable.class),
    PROTOBUF(String.class, Message.class);

    private final Class<?>[] classes;
    private SerializationMode(Class<?>... classes) {
      this.classes = classes;
    }

    public void checkClass(Class<?> classToCheck) {
      for(Class<?> c : classes) {
        if(c.isAssignableFrom(classToCheck)) {
          return;
        }
      }

      throw new UnsupportedOperationException(String.format("You are trying to serialize the class %s using the serialization mode %s.  This is not allowed.", classToCheck.getName(), this.name()));
    }
  }

  public static class CacheConfig<K, V>{
    private final Class<K> keyClass;
    private final Class<V> valueClass;
    private final String name;
    private final SerializationMode mode;

    public CacheConfig(Class<K> keyClass, Class<V> valueClass, String name, SerializationMode mode) {
      super();
      this.keyClass = keyClass;
      this.valueClass = valueClass;
      this.name = name;
      this.mode = mode;
    }

    public Class<K> getKeyClass() {
      return keyClass;
    }

    public Class<V> getValueClass() {
      return valueClass;
    }

    public SerializationMode getMode() {
      return mode;
    }

    public String getName() {
      return name;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((keyClass == null) ? 0 : keyClass.hashCode());
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((valueClass == null) ? 0 : valueClass.hashCode());
      return result;
    }

    public static <V> CacheConfigBuilder<String, V> newBuilder(Class<V> valueClass) {
      return newBuilder(String.class, valueClass);
    }

    public static <K, V> CacheConfigBuilder<K, V> newBuilder(Class<K> keyClass, Class<V> valueClass) {
      return new CacheConfigBuilder<K, V>(keyClass, valueClass);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CacheConfig<?, ?> other = (CacheConfig<?, ?>) obj;
      if (keyClass == null) {
        if (other.keyClass != null) {
          return false;
        }
      } else if (!keyClass.equals(other.keyClass)) {
        return false;
      }
      if (mode != other.mode) {
        return false;
      }
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      if (valueClass == null) {
        if (other.valueClass != null) {
          return false;
        }
      } else if (!valueClass.equals(other.valueClass)) {
        return false;
      }
      return true;
    }

  }

  public static class CacheConfigBuilder<K, V> {

    private Class<K> keyClass;
    private Class<V> valueClass;
    private String name;
    private SerializationMode mode = SerializationMode.DRILL_SERIALIZIABLE;

    private CacheConfigBuilder(Class<K> keyClass, Class<V> valueClass) {
      this.keyClass = keyClass;
      this.valueClass = valueClass;
      this.name = keyClass.getName();
    }

    public CacheConfigBuilder<K, V> mode(SerializationMode mode) {
      this.mode = mode;
      return this;
    }

    public CacheConfigBuilder<K, V> proto() {
      this.mode = SerializationMode.PROTOBUF;
      return this;
    }

    public CacheConfigBuilder<K, V> jackson() {
      this.mode = SerializationMode.JACKSON;
      return this;
    }

    public CacheConfigBuilder<K, V> drill() {
      this.mode = SerializationMode.DRILL_SERIALIZIABLE;
      return this;
    }


    public CacheConfigBuilder<K, V> name(String name) {
      this.name = name;
      return this;
    }

    public CacheConfig<K, V> build() {
      mode.checkClass(keyClass);
      mode.checkClass(valueClass);
      return new CacheConfig<K, V>(keyClass, valueClass, name, mode);
    }

  }

}
