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

import java.time.Instant;

import io.jhdf.HdfFile;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.store.hdf5.HDF5Utils;
import org.apache.drill.exec.vector.accessor.ValueWriter;

public class HDF5TimestampDataWriter extends HDF5DataWriter {

  private final long[] data;

  private final ValueWriter colWriter;

  // This constructor is used when the data is a 1D column.  The column is inferred from the datapath
  public HDF5TimestampDataWriter(HdfFile reader, WriterSpec writerSpec, String datapath) {
    super(reader, datapath);
    data = (long[]) reader.getDatasetByPath(datapath).getData();

    fieldName = HDF5Utils.getNameFromPath(datapath);
    colWriter = writerSpec.makeWriter(fieldName, TypeProtos.MinorType.TIMESTAMP, TypeProtos.DataMode.OPTIONAL);
  }

  @Override
  public boolean write() {
    if (!hasNext()) {
      return false;
    } else {
      colWriter.setTimestamp(Instant.ofEpochMilli(data[counter++]));
      return true;
    }
  }

  @Override
  public boolean hasNext() {
    return counter < data.length;
  }

  @Override
  public int getDataSize() {
    return data.length;
  }

  // Datasets of greater than 1D are not supported for timestamps
}
