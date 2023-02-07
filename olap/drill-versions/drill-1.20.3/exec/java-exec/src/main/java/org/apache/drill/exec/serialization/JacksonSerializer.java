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
package org.apache.drill.exec.serialization;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Objects;

public class JacksonSerializer<T> implements InstanceSerializer<T> {
  private final ObjectReader reader;
  private final ObjectWriter writer;

  public JacksonSerializer(final ObjectMapper mapper, final Class<T> klazz) {
    this.reader = mapper.readerFor(klazz);
    this.writer = mapper.writer();
  }

  @Override
  public T deserialize(final byte[] raw) throws IOException {
    return reader.readValue(raw);
  }

  @Override
  public byte[] serialize(final T instance) throws IOException {
    return writer.writeValueAsBytes(instance);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof JacksonSerializer && obj.getClass().equals(getClass())) {
      final JacksonSerializer<T> other = (JacksonSerializer<T>)obj;
      return Objects.equal(reader, other.reader) && Objects.equal(writer, other.writer);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(reader, writer);
  }
}
