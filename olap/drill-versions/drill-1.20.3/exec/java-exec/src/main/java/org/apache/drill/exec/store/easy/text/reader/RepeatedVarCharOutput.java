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
package org.apache.drill.exec.store.easy.text.reader;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is responsible for generating record batches for text file inputs. We generate
 * a record batch with a single vector of type repeated varchar vector. Each record is a single
 * value within the vector containing all the fields in the record as individual array elements.
 */
public class RepeatedVarCharOutput extends BaseFieldOutput {

  private static final Logger logger = LoggerFactory.getLogger(RepeatedVarCharOutput.class);

  private final ScalarWriter columnWriter;

  /**
   * Provide the row set loader (which must have just one repeated Varchar
   * column) and an optional array projection mask.
   *
   * @param loader row set loader
   * @param projectionMask array projection mask
   */
  public RepeatedVarCharOutput(RowSetLoader loader, boolean[] projectionMask) {
    super(loader,
        maxField(loader, projectionMask),
        projectionMask);
    ArrayWriter arrayWriter = writer.array(0);
    columnWriter = arrayWriter.scalar();
  }

  private static int maxField(RowSetLoader loader, boolean[] projectionMask) {

    // If the one and only field (`columns`) is not selected, then this
    // is a COUNT(*) or similar query. Select nothing.

    if (! loader.column(0).isProjected()) {
      return -1;
    }

    // If this is SELECT * or SELECT `columns` query, project all
    // possible fields.

    if (projectionMask == null) {
      return TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS;
    }

    // Else, this is a SELECT columns[x], columns[y], ... query.
    // Project only the requested element members (fields).

    int end = projectionMask.length - 1;
    while (end >= 0 && ! projectionMask[end]) {
      end--;
    }
    return end;
  }

  /**
   * Write the value into an array position. Rules:
   * <ul>
   * <li>If there is no projection mask, collect all columns.</li>
   * <li>If a selection mask is present, we previously found the index
   * of the last projection column (<tt>maxField</tt>). If the current
   * column is beyond that number, ignore the data and stop accepting
   * columns.</li>
   * <li>If the column is projected, add the data to the array.</li>
   * <li>If the column is not projected, add a blank value to the
   * array.</li>
   * </ul>
   * The above ensures that we leave no holes in the portion of the
   * array that is projected (by adding blank columns where needed),
   * and we just ignore columns past the end of the projected part
   * of the array. (No need to fill holes at the end.)
   */

  @Override
  public boolean endField() {

    // Skip the field if past the set of projected fields.

    if (currentFieldIndex > maxField) {
      return false;
    }

    // If the field is projected, save it.

    if (fieldProjected) {

      // Repeated var char will create as many entries as there are columns.
      // If this would exceed the maximum, issue an error. Note that we do
      // this only if all fields are selected; the same query will succeed if
      // the user does a COUNT(*) or SELECT columns[x], columns[y], ...

      if (currentFieldIndex > TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS) {
        throw UserException
          .unsupportedError()
          .message("Text file contains too many fields")
          .addContext("Limit", TextFormatPlugin.MAXIMUM_NUMBER_COLUMNS)
          .build(logger);
      }

      // Save the field.

      writeToVector();
    } else {

      // The field is not projected.
      // Must write a value into this array position, but
      // the value should be empty.

      columnWriter.setBytes(fieldBytes, 0);
    }

    // Return whether the rest of the fields should be read.

    return super.endField();
  }

  @Override
  protected ScalarWriter columnWriter() {
    return columnWriter;
  }
}
