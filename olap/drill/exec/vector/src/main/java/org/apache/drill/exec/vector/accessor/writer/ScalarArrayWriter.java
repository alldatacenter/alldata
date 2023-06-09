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
package org.apache.drill.exec.vector.accessor.writer;

import java.lang.reflect.Array;
import java.math.BigDecimal;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.BaseArrayWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractScalarWriterImpl.ScalarObjectWriter;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.joda.time.Period;

/**
 * Writer for a column that holds an array of scalars. This writer manages
 * the array itself. A type-specific child writer manages the elements within
 * the array. The overall row index (usually) provides the index into
 * the offset vector. An array-specific element index provides the index
 * into elements.
 * <p>
 * This class manages the offset vector directly. Doing so saves one read and
 * one write to direct memory per element value.
 * <p>
 * Provides generic write methods for testing and other times when
 * convenience is more important than speed.
 * <p>
 * The scalar writer for array-valued columns appends values: once a value
 * is written, it cannot be changed. As a result, writer methods have no item index;
 * each set advances the array to the next position. This is an abstract base class;
 * subclasses are generated for each repeated value vector type.
 */
public class ScalarArrayWriter extends BaseArrayWriter {

  /**
   * For scalar arrays, incrementing the element index and
   * committing the current value is done automatically since
   * there is exactly one value per array element.
   */
  public class ScalarElementWriterIndex extends ArrayElementWriterIndex {

    @Override
    public final void nextElement() { next(); }

    @Override
    public final void prevElement() { prev(); }
  }

  private final ScalarWriter elementWriter;

  public ScalarArrayWriter(ColumnMetadata schema,
      RepeatedValueVector vector, BaseScalarWriter baseElementWriter) {
    super(schema, vector.getOffsetVector(),
        new ScalarObjectWriter(baseElementWriter));

    // Save the writer from the scalar object writer created above
    // which may have wrapped the element writer in a type convertor.
    this.elementWriter = elementObjWriter.scalar();
  }

  public static ArrayObjectWriter build(ColumnMetadata schema,
      RepeatedValueVector repeatedVector, BaseScalarWriter baseElementWriter) {
    return new ArrayObjectWriter(
        new ScalarArrayWriter(schema, repeatedVector, baseElementWriter));
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) {
    elementIndex = new ScalarElementWriterIndex();
    super.bindIndex(index);
    elementObjWriter.events().bindIndex(elementIndex);
  }

  @Override
  public void save() {
    // No-op: done when writing each scalar value
    // May be called, however, by code that also supports
    // lists as a list does require an explicit save.
  }

  /**
   * Set a repeated vector based on a Java array of the proper
   * type. This function involves parsing the array type and so is
   * suitable only for test code. The array can be either a primitive
   * ({@code int [], say}) or a typed array of boxed values
   * ({@code Integer[], say}).
   */
  @Override
  public void setObject(Object array) {

    // Accept an empty array (of any type) to mean
    // an empty array of the type of this writer.
    if (array == null || Array.getLength(array) == 0) {

      // Assume null means a 0-element array since Drill does
      // not support null for the whole array.
      return;
    }
    final String objClass = array.getClass().getName();
    if (! objClass.startsWith("[")) {
      throw new IllegalArgumentException(
          String.format("Argument must be an array. Column `%s`, value = %s",
              schema().name(), array.toString()));
    }

    // Figure out type
    final char second = objClass.charAt(1);
    switch ( second ) {
    case  '[':

      // bytes is represented as an array of byte arrays.
      final char third = objClass.charAt(2);
      switch (third) {
      case 'B':
        setBytesArray((byte[][]) array);
        break;
      default:
        throw new IllegalArgumentException(
            String.format("Unknown Java array type: %s, for column `%s`",
                objClass, schema().name()));
      }
      break;
    case  'B':
      setByteArray((byte[]) array);
      break;
    case  'S':
      setShortArray((short[]) array);
      break;
    case  'I':
      setIntArray((int[]) array);
      break;
    case  'J':
      setLongArray((long[]) array);
      break;
    case  'F':
      setFloatArray((float[]) array);
      break;
    case  'D':
      setDoubleArray((double[]) array);
      break;
    case  'Z':
      setBooleanArray((boolean[]) array);
      break;
    case 'L':
      final int posn = objClass.indexOf(';');

      // If the array is of type Object, then we have no type info.
      final String memberClassName = objClass.substring(2, posn);
      if (memberClassName.equals(String.class.getName())) {
        setStringArray((String[]) array);
      } else if (memberClassName.equals(Period.class.getName())) {
        setPeriodArray((Period[]) array);
      } else if (memberClassName.equals(BigDecimal.class.getName())) {
        setBigDecimalArray((BigDecimal[]) array);
      } else if (memberClassName.equals(Integer.class.getName())) {
        setIntObjectArray((Integer[]) array);
      } else if (memberClassName.equals(Long.class.getName())) {
        setLongObjectArray((Long[]) array);
      } else if (memberClassName.equals(Double.class.getName())) {
        setDoubleObjectArray((Double[]) array);
      } else if (memberClassName.equals(Float.class.getName())) {
        setFloatObjectArray((Float[]) array);
      } else if (memberClassName.equals(Short.class.getName())) {
        setShortObjectArray((Short[]) array);
      } else if (memberClassName.equals(Byte.class.getName())) {
        setByteObjectArray((Byte[]) array);
      } else if (memberClassName.equals(Boolean.class.getName())) {
        setBooleanObjectArray((Boolean[]) array);
      } else {
        throw new IllegalArgumentException( "Unknown Java array type: " + memberClassName );
      }
      break;
    default:
      throw new IllegalArgumentException( "Unknown Java array type: " + objClass );
    }
  }

  public void setObjectArray(Object[] value) {
    for (int i = 0; i < value.length; i++) {
      final Object element = value[i];
      if (element != null) {
        elementWriter.setObject(element);
      }
    }
  }

  public void setBooleanArray(boolean[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setInt(value[i] ? 1 : 0);
    }
  }

  public void setBooleanObjectArray(Boolean[] value) {
    for (int i = 0; i < value.length; i++) {
      final Boolean element = value[i];
      if (element != null) {
        elementWriter.setBoolean(element);
      }
    }
  }

  public void setBytesArray(byte[][] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setBytes(value[i], value[i].length);
    }
  }

  public void setByteArray(byte[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setInt(value[i]);
    }
  }

  public void setByteObjectArray(Byte[] value) {
    for (int i = 0; i < value.length; i++) {
      final Byte element = value[i];
      if (element != null) {
        elementWriter.setInt(element);
      }
    }
  }

  public void setShortArray(short[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setInt(value[i]);
    }
  }

  public void setShortObjectArray(Short[] value) {
    for (int i = 0; i < value.length; i++) {
      final Short element = value[i];
      if (element != null) {
        elementWriter.setInt(element);
      }
    }
  }

  public void setIntArray(int[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setInt(value[i]);
    }
  }

  public void setIntObjectArray(Integer[] value) {
    for (int i = 0; i < value.length; i++) {
      final Integer element = value[i];
      if (element != null) {
        elementWriter.setInt(element);
      }
    }
  }

  public void setLongArray(long[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setLong(value[i]);
    }
  }

  public void setLongObjectArray(Long[] value) {
    for (int i = 0; i < value.length; i++) {
      final Long element = value[i];
      if (element != null) {
        elementWriter.setLong(element);
      }
    }
  }

  public void setFloatArray(float[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setDouble(value[i]);
    }
  }

  public void setFloatObjectArray(Float[] value) {
    for (int i = 0; i < value.length; i++) {
      final Float element = value[i];
      if (element != null) {
        elementWriter.setDouble(element);
      }
    }
  }

  public void setDoubleArray(double[] value) {
    for (int i = 0; i < value.length; i++) {
      elementWriter.setDouble(value[i]);
    }
  }

  public void setDoubleObjectArray(Double[] value) {
    for (int i = 0; i < value.length; i++) {
      final Double element = value[i];
      if (element != null) {
        elementWriter.setDouble(element);
      }
    }
  }

  public void setStringArray(String[] value) {
    for (int i = 0; i < value.length; i++) {
      final String element = value[i];
      if (element != null) {
        elementWriter.setString(element);
      }
    }
  }

  public void setPeriodArray(Period[] value) {
    for (int i = 0; i < value.length; i++) {
      final Period element = value[i];
      if (element != null) {
        elementWriter.setPeriod(element);
      }
    }
  }

  public void setBigDecimalArray(BigDecimal[] value) {
    for (int i = 0; i < value.length; i++) {
      final BigDecimal element = value[i];
      if (element != null) {
        elementWriter.setDecimal(element);
      }
    }
  }
}
