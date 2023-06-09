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
package org.apache.drill.exec.store.iceberg;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.iceberg.CombinedScanTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.StringJoiner;

@JsonSerialize(using = IcebergWork.IcebergWorkSerializer.class)
@JsonDeserialize(using = IcebergWork.IcebergWorkDeserializer.class)
public class IcebergWork {
  private final CombinedScanTask scanTask;

  public IcebergWork(CombinedScanTask scanTask) {
    this.scanTask = scanTask;
  }

  public CombinedScanTask getScanTask() {
    return this.scanTask;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IcebergWork that = (IcebergWork) o;
    return Objects.equals(scanTask, that.scanTask);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanTask);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", IcebergWork.class.getSimpleName() + "[", "]")
      .add("scanTask=" + scanTask)
      .toString();
  }

  /**
   * Special deserializer for {@link IcebergWork} class that deserializes
   * {@code scanTask} field from byte array string created using {@link java.io.Serializable}.
   */
  public static class IcebergWorkDeserializer extends StdDeserializer<IcebergWork> {

    private static final Logger logger = LoggerFactory.getLogger(IcebergWorkDeserializer.class);

    public IcebergWorkDeserializer() {
      super(IcebergWork.class);
    }

    @Override
    public IcebergWork deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      String scanTaskString = node.get(IcebergWorkSerializer.SCAN_TASK_FIELD).asText();
      try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(scanTaskString)))) {
        Object scanTask = ois.readObject();
        return new IcebergWork((CombinedScanTask) scanTask);
      } catch (ClassNotFoundException e) {
        logger.error(e.getMessage(), e);
      }

      return null;
    }
  }

  /**
   * Special serializer for {@link IcebergWork} class that serializes
   * {@code scanTask} field to byte array string created using {@link java.io.Serializable}
   * since {@link CombinedScanTask} doesn't use any Jackson annotations.
   */
  public static class IcebergWorkSerializer extends StdSerializer<IcebergWork> {

    public static final String SCAN_TASK_FIELD = "scanTask";

    public IcebergWorkSerializer() {
      super(IcebergWork.class);
    }

    @Override
    public void serialize(IcebergWork value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeStartObject();
      CombinedScanTask scanTask = value.getScanTask();
      try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
           ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
        objectOutputStream.writeObject(scanTask);
        gen.writeStringField(SCAN_TASK_FIELD, new String(Base64.getEncoder().encode(byteArrayOutputStream.toByteArray())));
      }
      gen.writeEndObject();
    }
  }

}
