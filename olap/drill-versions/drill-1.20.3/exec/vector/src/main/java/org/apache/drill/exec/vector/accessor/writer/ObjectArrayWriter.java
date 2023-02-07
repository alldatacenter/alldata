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

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.BaseArrayWriter;

/**
 * Writer for an array of either a map or another array. Here, the contents
 * are a structure and need explicit saves. State transitions in addition to the
 * base class are:
 *
 * <table border=1>
 * <tr><th>Public API</th><th>Array Event</th>
 *     <th>Offset Event</th><th>Element Event</th></tr>
 * <tr><td>save() (array)</td>
 *     <td>saveValue()</td>
 *     <td>saveValue()</td>
 *     <td>saveValue()</td></tr>
 * </table>
 *
 * This class is use for arrays of maps (and for arrays of arrays). When used
 * with a map, then we have a single offset vector pointing into a group of
 * arrays. Consider the simple case of a map of three scalars. Here, we have
 * a hybrid of the states discussed for the {@link BaseScalarWriter} and those
 * discussed for {@link OffsetVectorWriterImpl}. That is, the offset vector
 * points into one map element. The individual elements can we Behind,
 * Written or Unwritten, depending on the specific actions taken by the
 * client.
 * <p>
 * For example:<pre><code>
 *   Offset Vector      Vector A     Vector B    Vector C       Index
 *       |    |   + - >   |X| < lwa    |Y|         |Z|            8
 *  lw > |  8 | - +       | |          |Y|         |Z|            9
 *   v > | 10 | - - - >   | |          |Y|         |Z|           10
 *       |    |           | |          |Y| < lwb   |Z|           11
 *       |    |      v' > | |          | |         |Z| < lwc     12
 * </code></pre>
 * In the above:
 * <ul>
 * <li>The last write index, lw, for the current row points to the
 *     previous start position. (Recall that finishing the row writes the
 *     end position into the entry for the <i>next</i> row.</li>
 * <li>The top-level vector index, v, points to start position of
 *     the current row, which is offset 10 in all three data vectors.</li>
 * <li>The current array write position, v', is for the third element
 *     of the array that starts at position 10.</li>
 * <li>Since the row is active, the end position of the row has not yet
 *     been written, and so is blank in the offset vector.</li>
 * <li>The previous row had a two-element map array written, starting
 *     at offset 8 and ending at offset 9 (inclusive), identified as
 *     writing the next start offset (exclusive) into the following
 *     offset array slot.</li>
 * <li>Column A has not had data written since the first element of the
 *     previous row. It is currently in the Behind state with the last
 *     write position for A, lwa, pointing to the last write.</li>
 * <li>Column B is in the Unwritten state. A value was written for
 *     previous element in the map array, but not for the current element.
 *     We see this by the fact that the last write position for B, lwb,
 *     is one behind v'.</li>
 * <li>Column C has been written for the current array element and is
 *     in the Written state, with the last write position, lwc, pointing
 *     to the same location as v'.</li>
 * </ul>
 * Suppose we now write to Vector A and end the row:<pre><code>
 *   Offset Vector      Vector A     Vector B    Vector C       Index
 *       |    |   + - >   |X|          |Y|         |Z|            8
 *       |  8 | - +       |0|          |Y|         |Z|            9
 *  lw > | 10 | - - - >   |0|          |Y|         |Z|           10
 *   v > | 13 | - +       |0|          |Y| < lwb   |Z|           11
 *       |    |   |       |X| < lwa    | |         |Z| < lwc     12
 *       |    |   + - >   | |          | |         | | < v'      13
 * </code></pre>
 * Here:
 * <ul>
 * <li>Vector A has been back-filled and the last write index advanced.</li>
 * <li>Vector B is now in the Behind state. Vectors A and B are in the
 *     Unwritten state.</li>
 * <li>The end position has been written to the offset vector, the
 *     offset vector last write position has been advance, and the
 *     top-level vector offset has advanced.</li>
 * </ul>
 * All this happens automatically as part of the indexing mechanisms.
 * The key reason to understand this flow is to understand what happens
 * in vector overflow: unlike an array of scalars, in which the data
 * vector can never be in the Behind state, when we have an array of
 * maps then each vector can be in any of the scalar writer states.
 */
public class ObjectArrayWriter extends BaseArrayWriter {

  protected ObjectArrayWriter(ColumnMetadata schema,
      UInt4Vector offsetVector, AbstractObjectWriter elementWriter) {
    super(schema, offsetVector, elementWriter);
    elementIndex = new ArrayElementWriterIndex();
  }

  @Override
  public void save() {
    elementObjWriter.events().endArrayValue();
    elementIndex.next();
  }

  @Override
  public void setObject(Object array) {

    // Null array = 0-length array
    if (array == null) {
      return;
    }
    int size = Array.getLength(array);
    for (int i = 0; i < size; i++) {
      Object value = Array.get(array, i);
      if (value != null) {
        elementObjWriter.setObject(value);
      }
      save();
    }
  }
}
