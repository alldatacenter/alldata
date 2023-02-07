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
package org.apache.drill.exec.physical.impl.scan.project;

import java.util.List;

import org.apache.drill.exec.physical.resultSet.ResultVectorCache;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.accessor.TupleWriter;

/**
 * Populate metadata columns either file metadata (AKA "implicit
 * columns") or directory metadata (AKA "partition columns.") In both
 * cases the column type is nullable Varchar and the column value
 * is predefined by the projection planner; this class just copies
 * that value into each row.
 * <p>
 * The values for the columns appear in the column definitions.
 * This works because the metadata columns are re-resolved for
 * for each file, picking up file-specific values. In some cases,
 * a column might not even have a value (such as a reference to
 * a dirN level that isn't defined for a particular file.
 * <p>
 * That said, this class is agnostic about the source and meaning
 * of the columns: it only cares that the columns are of type
 * nullable Varchar and that the values are in the column nodes.
 */

public class ConstantColumnLoader extends StaticColumnLoader {

  public interface ConstantColumnSpec {
    String name();
    MaterializedField schema();
    String value();
  }

  private final String values[];
  private final List<? extends ConstantColumnSpec> constantCols;

  public ConstantColumnLoader(ResultVectorCache vectorCache,
      List<? extends ConstantColumnSpec> defns) {
    super(vectorCache);

    // Populate the loader schema from that provided.
    // Cache values for faster access.
    // TODO: Rewrite to specify schema up front

    constantCols = defns;
    RowSetLoader schema = loader.writer();
    values = new String[defns.size()];
    for (int i = 0; i < defns.size(); i++) {
      ConstantColumnSpec defn  = defns.get(i);
      values[i] = defn.value();
      schema.addColumn(defn.schema());
    }
  }

  @Override
  public VectorContainer load(int rowCount) {
    loader.startBatch();
    RowSetLoader writer = loader.writer();
    for (int i = 0; i < rowCount; i++) {
      writer.start();
      loadRow(writer);
      writer.save();
    }
    return loader.harvest();
  }

  /**
   * Populate static vectors with the defined static values.
   *
   * @param writer writer for a tuple
   */

  private void loadRow(TupleWriter writer) {
    for (int i = 0; i < values.length; i++) {

      // Set the column (of any type) to null if the string value
      // is null.

      if (values[i] == null) {
        writer.scalar(i).setNull();
      } else {
        // Else, set the static (string) value.

        writer.scalar(i).setString(values[i]);
      }
    }
  }

  public List<? extends ConstantColumnSpec> columns() { return constantCols; }
}
