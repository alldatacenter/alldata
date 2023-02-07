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

import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.vector.accessor.ValueWriter;

/**
 * For CSV files without headers, but with a provided schema,
 * handles the case where extra fields appear in the file beyond
 * the columns enumerated in the schema. These fields are ignored.
 */
public class ConstrainedFieldOutput extends FieldVarCharOutput {

  ConstrainedFieldOutput(RowSetLoader writer, ValueWriter[] colWriters) {
    super(writer, colWriters);
  }

  @Override
  protected void writeToVector() {

    // Reject columns past the known schema.
    if (currentFieldIndex < colWriters.length) {
      super.writeToVector();
    } else {
      currentDataPointer = 0;
    }
  }
}
