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
package org.apache.drill.exec.record;

import java.io.IOException;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class MajorTypeSerDe {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MajorTypeSerDe.class);


  @SuppressWarnings("serial")
  public static class De extends StdDeserializer<MajorType> {

    public De() {
      super(MajorType.class);
    }

    @Override
    public MajorType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException {
      return jp.readValueAs(MajorTypeHolder.class).getMajorType();
    }

  }

  @SuppressWarnings("serial")
  public static class Se extends StdSerializer<MajorType> {

    public Se() {
      super(MajorType.class);
    }

    @Override
    public void serialize(MajorType value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
        JsonGenerationException {
      MajorTypeHolder holder = MajorTypeHolder.get(value);
      jgen.writeObject(holder);
    }

  }

  @JsonInclude(Include.NON_NULL)
  public static class MajorTypeHolder{
    @JsonProperty("type") public MinorType minorType;
    public DataMode mode;
    public Integer width;
    public Integer precision;
    public Integer scale;

    @JsonCreator
    public MajorTypeHolder(@JsonProperty("type") MinorType minorType, @JsonProperty("mode") DataMode mode, @JsonProperty("width") Integer width, @JsonProperty("precision") Integer precision, @JsonProperty("scale") Integer scale) {
      super();
      this.minorType = minorType;
      this.mode = mode;
      this.width = width;
      this.precision = precision;
      this.scale = scale;
    }

    private MajorTypeHolder() {}

    @JsonIgnore
    public MajorType getMajorType() {
      MajorType.Builder b = MajorType.newBuilder();
      b.setMode(mode);
      b.setMinorType(minorType);
      if (precision != null) {
        b.setPrecision(precision);
      }
      if (width != null) {
        b.setWidth(width);
      }
      if (scale != null) {
        b.setScale(scale);
      }
      return b.build();
    }

    public static MajorTypeHolder get(MajorType mt) {
      MajorTypeHolder h = new MajorTypeHolder();
      h.minorType = mt.getMinorType();
      h.mode = mt.getMode();
      if (mt.hasPrecision()) {
        h.precision = mt.getPrecision();
      }
      if (mt.hasScale()) {
        h.scale = mt.getScale();
      }
      if (mt.hasWidth()) {
        h.width = mt.getWidth();
      }
      return h;
    }
  }

}
