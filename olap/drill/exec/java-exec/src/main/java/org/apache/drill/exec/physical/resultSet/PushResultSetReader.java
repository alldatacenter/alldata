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
package org.apache.drill.exec.physical.resultSet;

import org.apache.drill.exec.physical.rowSet.RowSetReader;

/**
 * Push-based result set reader, in which the caller obtains batches
 * and registers them with the implementation. The client thus is responsible
 * for detecting the end of batches and releasing memory. General protocol:
 * <p>
 * <ul>
 * <li>Create an instance and bind it to a batch source.</li>
 * <li>Obtain a batch, typically by having it passed in.</li>
 * <li>Call {@link #start()} to obtain a reader for that batch.</li>
 * <li>Iterate over the rows.</li>
 * <li>Release memory for the batch.</li>
 * </ul>
 * <p>
 * In Drill,
 * batches may have the same or different schemas. Each call to
 * {@link #start()} prepares a {@link RowSetReader} to use for
 * the available batch. If the batch has the same schema as the previous,
 * then the existing reader is simply repositioned at the start of the
 * batch. If the schema changed (or this is the first batch), then a
 * new reader is created. Thus, the client should not assume that the
 * same reader is available across calls. However, if it is useful to
 * cache column writers, simply check if the reader returned from
 * {@code start()} is the same as the previous one. If so, the column
 * writers are also the same.
 */
public interface PushResultSetReader {
  RowSetReader start();
}
