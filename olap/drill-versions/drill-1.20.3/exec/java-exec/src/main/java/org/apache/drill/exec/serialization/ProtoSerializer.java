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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.protostuff.JsonIOUtil;
import io.protostuff.Schema;
import org.apache.drill.shaded.guava.com.google.common.base.Objects;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import com.google.protobuf.Message;

public class ProtoSerializer<T, B extends Message.Builder> implements InstanceSerializer<T> {
  private final Schema<B> readSchema;
  private final Schema<T> writeSchema;

  public ProtoSerializer(final Schema<B> readSchema, final Schema<T> writeSchema) {
    this.readSchema = Preconditions.checkNotNull(readSchema);
    this.writeSchema = Preconditions.checkNotNull(writeSchema);
  }

  @Override
  public T deserialize(final byte[] raw) throws IOException {
    final B builder = readSchema.newMessage();
    JsonIOUtil.mergeFrom(raw, builder, readSchema, false);
    return (T)builder.build();
  }

  @Override
  public byte[] serialize(final T instance) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    JsonIOUtil.writeTo(out, instance, writeSchema, false);
    return out.toByteArray();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(readSchema, writeSchema);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ProtoSerializer && obj.getClass().equals(getClass())) {
      final ProtoSerializer<T, B> other = (ProtoSerializer<T, B>)obj;
      return Objects.equal(readSchema, other.readSchema) && Objects.equal(writeSchema, other.writeSchema);
    }
    return false;
  }
}
