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
import io.jhdf.object.datatype.CompoundDataType;
import io.jhdf.object.datatype.CompoundDataType.CompoundDataMember;
import org.apache.drill.common.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class HDF5MapDataWriter extends HDF5DataWriter {

  private static final Logger logger = LoggerFactory.getLogger(HDF5MapDataWriter.class);
  private static final String UNSAFE_SPACE_SEPARATOR = " ";
  private static final String SAFE_SPACE_SEPARATOR = "_";
  private final List<HDF5DataWriter> dataWriters;
  private final List<CompoundDataMember> data;

  public HDF5MapDataWriter(HdfFile reader, WriterSpec writerSpec, String datapath) {
    super(reader, datapath);
    // Get the members of the dataset
    compoundData = (LinkedHashMap<String, ?>)reader.getDatasetByPath(datapath).getData();
    data = ((CompoundDataType) reader.getDatasetByPath(datapath).getDataType()).getMembers();
    dataWriters = new ArrayList<>();

    try {
      getDataWriters(writerSpec);
    } catch (Exception e) {
      throw UserException
        .dataReadError(e)
        .addContext("Error writing compound field", datapath)
        .addContext(writerSpec.errorContext)
        .build(logger);
    }
  }

  @Override
  public boolean write() {
    if (hasNext()) {
      // Loop through the columns and write the columns
      for (HDF5DataWriter dataWriter : dataWriters) {
        dataWriter.write();
      }
      counter++;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean hasNext() {
    return counter < dataWriters.get(0).getDataSize();
  }

  /**
   * Populates the ArrayList of DataWriters. Since HDF5 Maps contain homogeneous
   * columns, it is fine to get the first row, and iterate through the columns
   * to get the data types and build the schema accordingly.
   */
  private void getDataWriters(WriterSpec writerSpec) {

    for (CompoundDataMember dataMember : data) {
      String dataType = dataMember.getDataType().getJavaType().getName();
      String fieldName = dataMember.getName();
      switch (dataType) {
        case "byte":
          dataWriters.add(new HDF5ByteDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (byte[])compoundData.get(fieldName)));
          break;
        case "short":
          dataWriters.add(new HDF5SmallIntDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (short[])compoundData.get(fieldName)));
          break;
        case "int":
          dataWriters.add(new HDF5IntDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (int[])compoundData.get(fieldName)));
          break;
        case "long":
          dataWriters.add(new HDF5LongDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (long[])compoundData.get(fieldName)));
          break;
        case "double":
          dataWriters.add(new HDF5DoubleDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (double[])compoundData.get(fieldName)));
          break;
        case "float":
          dataWriters.add(new HDF5FloatDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR), (float[])compoundData.get(fieldName)));
          break;
        case "java.lang.String":
          dataWriters.add(new HDF5StringDataWriter(reader, writerSpec, fieldName.replace(UNSAFE_SPACE_SEPARATOR, SAFE_SPACE_SEPARATOR),  (String[])compoundData.get(fieldName)));
          break;
        default:
          // Log unknown data type
          logger.warn("Drill cannot process data type {} in compound fields.", dataType);
          break;
      }
    }
  }

  /**
   * Returns true if the data writer is a compound writer, false if not.
   * @return boolean true if the data writer is a compound writer, false if not.
   */
  @Override
  public boolean isCompound() {
    return true;
  }

  @Override
  public int getDataSize() {
    return dataWriters.size();
  }
}
