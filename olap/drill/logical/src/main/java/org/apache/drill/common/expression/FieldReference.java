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
package org.apache.drill.common.expression;

import java.io.IOException;

import org.apache.drill.common.expression.FieldReference.De;
import org.apache.drill.common.expression.FieldReference.Se;
import org.apache.drill.common.expression.PathSegment.NameSegment;
import org.apache.drill.common.types.TypeProtos.MajorType;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonSerialize(using = Se.class)
@JsonDeserialize(using = De.class)
public class FieldReference extends SchemaPath {
  private MajorType overrideType;

  public FieldReference(SchemaPath sp) {
    super(sp);
  }

  public FieldReference(CharSequence value) {
    this(value, ExpressionPosition.UNKNOWN);
  }

  public FieldReference(CharSequence value, ExpressionPosition pos) {
    super(new NameSegment(value), pos);
  }

  public FieldReference(String value, ExpressionPosition pos, MajorType dataType) {
    this(value, pos);
    this.overrideType = dataType;
  }

  /**
   * Create a {@link FieldReference} given an unquoted name. (Note: the
   * name here is a misnomer: the name may have been quoted in SQL, but
   * must be unquoted when passed in here.)
   *
   * @param safeString the unquoted field reference
   * @return the field reference expression
   */
  public static FieldReference getWithQuotedRef(CharSequence safeString) {
    return new FieldReference(safeString, ExpressionPosition.UNKNOWN);
  }

  @Override
  public MajorType getMajorType() {
    if (overrideType == null) {
      return super.getMajorType();
    } else {
      return overrideType;
    }
  }

  @SuppressWarnings("serial")
  public static class De extends StdDeserializer<FieldReference> {

    public De() {
      super(FieldReference.class);
    }

    @Override
    public FieldReference deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException {
      String ref = this._parseString(jp, ctxt);
      ref = ref.replace("`", "");
      return new FieldReference(ref, ExpressionPosition.UNKNOWN);
    }
  }

  @SuppressWarnings("serial")
  public static class Se extends StdSerializer<FieldReference> {

    public Se() {
      super(FieldReference.class);
    }

    @Override
    public void serialize(FieldReference value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
        JsonGenerationException {
      jgen.writeString('`' + value.getRootSegment().getNameSegment().getPath() + '`');
    }
  }
}
