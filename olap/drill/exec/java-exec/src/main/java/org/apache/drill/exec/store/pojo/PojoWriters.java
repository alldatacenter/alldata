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
package org.apache.drill.exec.store.pojo;

import io.netty.buffer.DrillBuf;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.Float4Vector;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.drill.exec.vector.NullableFloat4Vector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableVarCharVector;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.exec.vector.VarDecimalVector;

public class PojoWriters {

  /**
   * Creates matching writer to the given field type.
   *
   * @param type field type
   * @param fieldName field name
   * @param buffer drill buffer
   * @return pojo writer
   * @throws ExecutionSetupException in case if writer was not found for the given type
   */
  public static PojoWriter getWriter(Class<?> type, String fieldName, DrillBuf buffer) throws ExecutionSetupException {

    if (type == Integer.class) {
      return new NIntWriter(fieldName);
    } else if (type == Long.class) {
      return new NBigIntWriter(fieldName);
    } else if (type == Boolean.class) {
      return new NBooleanWriter(fieldName);
    } else if (type == Float.class) {
      return new NFloatWriter(fieldName);
    } else if (type == Double.class) {
      return new NDoubleWriter(fieldName);
    } else if (type.isEnum()) {
      return new EnumWriter(fieldName, buffer);
    } else if (type == String.class) {
      return new StringWriter(fieldName, buffer);
    } else if (type == BigDecimal.class) {
      return new DecimalWriter(fieldName);
    } else if (type == Timestamp.class) {
      return new NTimeStampWriter(fieldName);
      // primitives
    } else if (type == int.class) {
      return new IntWriter(fieldName);
    } else if (type == float.class) {
      return new FloatWriter(fieldName);
    } else if (type == double.class) {
      return new DoubleWriter(fieldName);
    } else if (type == boolean.class) {
      return new BitWriter(fieldName);
    } else if (type == long.class) {
      return new LongWriter(fieldName);
    }

    throw new ExecutionSetupException(String.format("PojoRecordReader doesn't yet support conversions from the type [%s].", type));
  }

  /**
   * Pojo writer for int. Does not expect to write null value.
   */
  public static class IntWriter extends AbstractPojoWriter<IntVector> {

    public IntWriter(String fieldName) {
      super(fieldName, Types.required(MinorType.INT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      vector.getMutator().setSafe(outboundIndex, (int) value);
    }
  }

  /**
   * Pojo writer for boolean. Does not expect to write null value.
   */
  public static class BitWriter extends AbstractPojoWriter<BitVector> {

    public BitWriter(String fieldName) {
      super(fieldName, Types.required(MinorType.BIT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      vector.getMutator().setSafe(outboundIndex, (boolean) value ? 1 : 0);
    }

  }

  /**
   * Pojo writer for long. Does not expect to write null value.
   */
  public static class LongWriter extends AbstractPojoWriter<BigIntVector> {

    public LongWriter(String fieldName) {
      super(fieldName, Types.required(MinorType.BIGINT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      vector.getMutator().setSafe(outboundIndex, (long) value);
    }

  }

  /**
   * Pojo writer for float. Does not expect to write null value.
   */
  public static class FloatWriter extends AbstractPojoWriter<Float4Vector> {

    public FloatWriter(String fieldName) {
      super(fieldName, Types.required(MinorType.FLOAT4));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      vector.getMutator().setSafe(outboundIndex, (float) value);
    }

  }
  /**
   * Pojo writer for double. Does not expect to write null value.
   */
  public static class DoubleWriter extends AbstractPojoWriter<Float8Vector> {

    public DoubleWriter(String fieldName) {
      super(fieldName, Types.required(MinorType.FLOAT8));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      vector.getMutator().setSafe(outboundIndex, (double) value);
    }

  }

  /**
   * Pojo writer for decimal. If null is encountered does not write it.
   */
  public static class DecimalWriter extends AbstractPojoWriter<VarDecimalVector> {

    public DecimalWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.VARDECIMAL));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (BigDecimal) value);
      }
    }

  }

  /**
   * Parent class for String and Enum writers. Writes data using nullable varchar holder.
   */
  private abstract static class AbstractStringWriter extends AbstractPojoWriter<NullableVarCharVector> {
    private DrillBuf data;
    private final NullableVarCharHolder holder = new NullableVarCharHolder();

    public AbstractStringWriter(String fieldName, DrillBuf managedBuf) {
      super(fieldName, Types.optional(MinorType.VARCHAR));
      this.data = managedBuf;
      ensureLength(100);
    }

    void ensureLength(int len) {
      data = data.reallocIfNeeded(len);
    }

    public void writeString(String s, int outboundIndex) {
      holder.isSet = 1;
      byte[] bytes = s.getBytes(Charsets.UTF_8);
      ensureLength(bytes.length);
      data.clear();
      data.writeBytes(bytes);
      holder.buffer = data;
      holder.start = 0;
      holder.end = bytes.length;
      vector.getMutator().setSafe(outboundIndex, holder);
    }
  }

  /**
   * Pojo writer for Enum. If null is encountered does not write it.
   */
  public static class EnumWriter extends AbstractStringWriter{
    public EnumWriter(String fieldName, DrillBuf managedBuf) {
      super(fieldName, managedBuf);
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value == null) {
        return;
      }
      writeString(((Enum<?>) value).name(), outboundIndex);
    }
  }

  /**
   * Pojo writer for String. If null is encountered does not write it.
   */
  public static class StringWriter extends AbstractStringWriter {
    public StringWriter(String fieldName, DrillBuf managedBuf) {
      super(fieldName, managedBuf);
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        writeString((String) value, outboundIndex);
      }
    }
  }

  /**
   * Pojo writer for Integer. If null is encountered does not write it.
   */
  public static class NIntWriter extends AbstractPojoWriter<NullableIntVector> {

    public NIntWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.INT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (Integer) value);
      }
    }

  }

  /**
   * Pojo writer for Long. If null is encountered does not write it.
   */
  public static class NBigIntWriter extends AbstractPojoWriter<NullableBigIntVector> {

    public NBigIntWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.BIGINT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (Long) value);
      }
    }

  }

  /**
   * Pojo writer for Boolean. If null is encountered does not write it.
   */
  public static class NBooleanWriter extends AbstractPojoWriter<NullableBitVector> {

    public NBooleanWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.BIT));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (Boolean) value ? 1 : 0);
      }
    }

  }

  /**
   * Pojo writer for Float. If null is encountered does not write it.
   */
  public static class NFloatWriter extends AbstractPojoWriter<NullableFloat4Vector> {

    public NFloatWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.FLOAT4));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (Float) value);
      }
    }

  }

  /**
   * Pojo writer for Double. If null is encountered does not write it.
   */
  public static class NDoubleWriter extends AbstractPojoWriter<NullableFloat8Vector> {

    public NDoubleWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.FLOAT8));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, (Double) value);
      }
    }

  }

  /**
   * Pojo writer for Timestamp. If null is encountered does not write it.
   */
  public static class NTimeStampWriter extends AbstractPojoWriter<NullableTimeStampVector> {

    public NTimeStampWriter(String fieldName) {
      super(fieldName, Types.optional(MinorType.TIMESTAMP));
    }

    @Override
    public void writeField(Object value, int outboundIndex) {
      if (value != null) {
        vector.getMutator().setSafe(outboundIndex, ((Timestamp) value).getTime());
      }
    }
  }
}
