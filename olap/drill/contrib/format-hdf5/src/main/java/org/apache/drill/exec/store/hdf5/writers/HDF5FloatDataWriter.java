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

package org.apache.drill.exec.store.hdf5.writers;

import io.jhdf.HdfFile;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.store.hdf5.HDF5Utils;
import org.apache.drill.exec.vector.accessor.ValueWriter;

public class HDF5FloatDataWriter extends HDF5DataWriter {

  private final float[] data;

  private final ValueWriter colWriter;

  // This constructor is used when the data is a 1D column.  The column is inferred from the datapath
  public HDF5FloatDataWriter(HdfFile reader, WriterSpec writerSpec, String datapath) {
    super(reader, datapath);
    data = (float[]) reader.getDatasetByPath(datapath).getData();
    fieldName = HDF5Utils.getNameFromPath(datapath);
    colWriter = writerSpec.makeWriter(fieldName, TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL);
  }

  // This constructor is used when the data is part of a 2D array.  In this case the column name is provided in the constructor
  public HDF5FloatDataWriter(HdfFile reader, WriterSpec writerSpec, String datapath, String fieldName, int currentColumn) {
    super(reader, datapath, fieldName, currentColumn);
    // Get dimensions
    int[] dimensions = reader.getDatasetByPath(datapath).getDimensions();
    float[][] tempData = new float[0][];

    if (dimensions.length == 2) {
      tempData = transpose((float[][]) reader.getDatasetByPath(datapath).getData());
    } else {
      tempData = transpose(HDF5Utils.toFloatMatrix((Object[])reader.getDatasetByPath(datapath).getData()));
    }
    data = tempData[currentColumn];
    colWriter = writerSpec.makeWriter(fieldName, TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL);
  }

  public HDF5FloatDataWriter(HdfFile reader, WriterSpec writerSpec, String fieldName, float[] tempListData) {
    super(reader, null);
    this.fieldName = fieldName;
    data = tempListData;
    colWriter = writerSpec.makeWriter(fieldName, TypeProtos.MinorType.FLOAT8, TypeProtos.DataMode.OPTIONAL);
  }


  @Override
  public boolean write() {
    if (!hasNext()) {
      return false;
    } else {
      colWriter.setDouble(data[counter++]);
      return true;
    }
  }

  @Override
  public boolean hasNext() {
    return counter < data.length;
  }

  /**
   * Transposes the input matrix by flipping a matrix over its diagonal by switching the row and column
   * indices of the matrix by producing another matrix.
   * @param matrix The input matrix to be transposed
   * @return The transposed matrix.
   */
  private float[][] transpose (float[][] matrix) {
    if (matrix == null || matrix.length == 0) {
      return matrix;
    }
    int width = matrix.length;
    int height = matrix[0].length;

    float[][] transposedMatrix = new float[height][width];

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        transposedMatrix[y][x] = matrix[x][y];
      }
    }
    return transposedMatrix;
  }

  @Override
  public int getDataSize() {
    return data.length;
  }
}
