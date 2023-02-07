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
package org.apache.drill.exec.coord;

import java.io.IOException;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class DrillbitEndpointSerDe {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillbitEndpointSerDe.class);

  public static class De extends StdDeserializer<DrillbitEndpoint> {

    public De() {
      super(DrillbitEndpoint.class);
    }

    @Override
    public DrillbitEndpoint deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException {
      return DrillbitEndpoint.parseFrom(jp.getBinaryValue());
    }


  }


  public static class Se extends StdSerializer<DrillbitEndpoint> {

    public Se() {
      super(DrillbitEndpoint.class);
    }

    @Override
    public void serialize(DrillbitEndpoint value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
        JsonGenerationException {
      jgen.writeBinary(value.toByteArray());
    }

  }
}
