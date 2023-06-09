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

package org.apache.drill.exec.store.hdf5;

import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.object.datatype.DataType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.drill.common.types.TypeProtos.MinorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HDF5Utils {
  private static final Logger logger = LoggerFactory.getLogger(HDF5Utils.class);

  /*
   * This regex is used to extract the final part of an HDF5 path, which is the name of the data field or column.
   * While these look like file paths, they are fully contained within HDF5. This regex would extract part3 from:
   * /part1/part2/part3
   */
  private static final Pattern PATH_PATTERN = Pattern.compile("/*.*/(.+?)$");


  /**
   * This function returns and HDF5Attribute object for use when Drill maps the attributes.
   *
   * @param pathName The path to retrieve attributes from
   * @param key The key for the specific attribute you are retrieving
   * @param hdf5File The hdfFile reader object for the file you are querying
   * @return HDF5Attribute The attribute from the path with the key that was requested.
   */
  public static HDF5Attribute getAttribute(String pathName, String key, HdfFile hdf5File) {
    if (pathName.equals("")) {
      pathName = "/";
    }
    if (hdf5File.getByPath(pathName) == null) {
      return null;
    }

    if (key.equals("dimensions")) {
      int[] dimensions = hdf5File.getDatasetByPath(pathName).getDimensions();
      ArrayUtils.reverse(dimensions);
      return new HDF5Attribute(MinorType.LIST, "dimensions", dimensions);
    }

    if (key.equals("dataType")) {
      String typeName = hdf5File.getDatasetByPath(pathName).getDataType().getJavaType().getName();
      return new HDF5Attribute(getDataType(hdf5File.getDatasetByPath(pathName).getDataType()), "DataType", typeName);
    }
    if (hdf5File.getByPath(pathName).getAttribute(key) == null) {
      return null;
    }

    Attribute attribute = hdf5File.getByPath(pathName).getAttribute(key);
    Class<?> type = hdf5File.getByPath(pathName).getAttribute(key).getJavaType();

    if (type.isAssignableFrom(long[].class)) {
      return new HDF5Attribute(MinorType.BIGINT, key, attribute.getData(), true);
    } else if (type.isAssignableFrom(int[].class)) {
      return new HDF5Attribute(MinorType.INT, key, attribute.getData(), true);
    } else if (type.isAssignableFrom(short[].class)) {
      return new HDF5Attribute(MinorType.INT, key, attribute.getData(), true);
    } else if (type.isAssignableFrom(byte[].class)) {
      return new HDF5Attribute(MinorType.INT, key,  attribute.getData(), true);
    } else if (type.isAssignableFrom(double[].class)) {
      return new HDF5Attribute(MinorType.FLOAT8, key, attribute.getData(), true);
    } else if (type.isAssignableFrom(float[].class)) {
      return new HDF5Attribute(MinorType.FLOAT8, key,  attribute.getData(), true);
    } else if (type.isAssignableFrom(String[].class)) {
      return new HDF5Attribute(MinorType.VARCHAR, key,  attribute.getData(), true);
    } else if (type.isAssignableFrom(java.lang.Long.class)) {
      return new HDF5Attribute(MinorType.BIGINT, key, attribute.getData());
    } else if (type.isAssignableFrom(java.lang.Integer.class)) {
      return new HDF5Attribute(MinorType.INT, key,  attribute.getData());
    } else if (type.isAssignableFrom(java.lang.Short.class)) {
      return new HDF5Attribute(MinorType.INT, key, attribute.getData());
    } else if (type.isAssignableFrom(java.lang.Byte.class)) {
      return new HDF5Attribute(MinorType.INT, key, attribute.getData());
    } else if (type.isAssignableFrom(java.lang.Double.class)) {
      return new HDF5Attribute(MinorType.FLOAT8, key, attribute.getData());
    } else if (type.isAssignableFrom(float.class)) {
      return new HDF5Attribute(MinorType.FLOAT4, key, attribute.getData());
    } else if (type.isAssignableFrom(String.class)) {
      return new HDF5Attribute(MinorType.VARCHAR, key, attribute.getData());
    } else if (type.isAssignableFrom(boolean.class)) {
      return new HDF5Attribute(MinorType.BIT, key, attribute.getData());
    }/*else if (type.isAssignableFrom(HDF5EnumerationValue.class)) {
      // Convert HDF5 Enum to String
      return new HDF5Attribute(MinorType.GENERIC_OBJECT, key, attribute.getData());
    }*/ else if (type.isAssignableFrom(BitSet.class)) {
      return new HDF5Attribute(MinorType.BIT, key, attribute.getData());
    }

    logger.warn("Reading attributes of type {} not yet implemented.", attribute.getJavaType());
    return null;
  }

  /**
   * This function returns the Drill data type of a given HDF5 dataset.
   * @param dataType The input data set.
   * @return MinorType The Drill data type of the dataset in question
   */
  public static MinorType getDataType(DataType dataType) {

    Class<?> type = dataType.getJavaType();
    if (type == null) {
      logger.warn("Datasets of type {} not implemented.", dataType.getDataClass());
      //Fall back to string
      return MinorType.VARCHAR;
    } else if (type.isAssignableFrom(long.class)) {
      return MinorType.BIGINT;
    } else if (type.isAssignableFrom(short.class)) {
      return MinorType.SMALLINT;
    } else if (type.isAssignableFrom(byte.class)) {
      return MinorType.TINYINT;
    } else if (type.isAssignableFrom(int.class)) {
      return MinorType.INT;
    } else if (type.isAssignableFrom(float.class)) {
      return MinorType.FLOAT4;
    } else if (type.isAssignableFrom(double.class)) {
      return MinorType.FLOAT8;
    } else if (type.isAssignableFrom(String.class)) {
      return MinorType.VARCHAR;
    } else if (type.isAssignableFrom(java.util.Date.class) || type.isAssignableFrom(java.lang.Long.class)) {
      return MinorType.TIMESTAMP;
    } else if (type.isAssignableFrom(boolean.class) || type.isAssignableFrom(BitSet.class)) {
      return MinorType.BIT;
    } else if (type.isAssignableFrom(Map.class)) {
      return MinorType.MAP;
    } else if (type.isAssignableFrom(Enum.class)) {
      return MinorType.GENERIC_OBJECT;
    }
    return MinorType.GENERIC_OBJECT;
  }

  /**
   * This function gets the type of dataset
   * @param path The path of the dataset
   * @param reader The HDF5 reader
   * @return The data type
   */
  public static Class<?> getDatasetClass(String path, HdfFile reader) {
    return reader.getDatasetByPath(path).getJavaType();
  }

  /**
   * This helper function returns the name of a HDF5 record from a data path
   *
   * @param path Path to HDF5 data
   * @return String name of data
   */
  public static String getNameFromPath(String path) {
    if( path == null) {
      return null;
    }
    // Now create matcher object.
    Matcher m = PATH_PATTERN.matcher(path);
    if (m.find()) {
      return m.group(1);
    } else {
      return "";
    }
  }

  public static Object[] toMatrix(Object[] inputArray) {
    return flatten(inputArray).toArray();
  }

  public static boolean[][] toBooleanMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((boolean[][])input[0]).length;

    boolean[][] result = new boolean[cols][rows];

    for (int i = 0; i <  rows; i++) {
      boolean[] row = (boolean[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static byte[][] toByteMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((byte[])input[0]).length;

    byte[][] result = new byte[cols][rows];

    for (int i = 0; i <  rows; i++) {
      byte[] row = (byte[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static short[][] toShortMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((short[])input[0]).length;

    short[][] result = new short[cols][rows];

    for (int i = 0; i <  rows; i++) {
      short[] row = (short[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }


  public static int[][] toIntMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((int[])input[0]).length;

    int[][] result = new int[cols][rows];

    for (int i = 0; i <  rows; i++) {
      int[] row = (int[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static long[][] toLongMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((long[])input[0]).length;

    long[][] result = new long[cols][rows];

    for (int i = 0; i <  rows; i++) {
      long[] row = (long[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static float[][] toFloatMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((float[])input[0]).length;

    float[][] result = new float[cols][rows];

    for (int i = 0; i <  rows; i++) {
      float[] row = (float[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static double[][] toDoubleMatrix(Object[] inputArray) {
    Object[] input = flatten(inputArray).toArray();
    int rows = input.length;
    int cols = ((double[])input[0]).length;

    double[][] result = new double[cols][rows];

    for (int i = 0; i <  rows; i++) {
      double[] row = (double[])input[i];
      for (int j = 0; j < cols; j++) {
        result[j][i] = row[j];
      }
    }
    return result;
  }

  public static Stream<Object> flatten(Object[] array) {
    return Arrays.stream(array)
      .flatMap(o -> o instanceof Object[]? flatten((Object[])o): Stream.of(o));
  }
}
