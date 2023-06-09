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
package org.apache.drill.exec.physical.impl.scan.v3;

import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;

/**
 * Creates a batch reader on demand. Unlike earlier versions of Drill,
 * this framework creates readers one by one, when they are needed.
 * Doing so avoids excessive resource demands that come from creating
 * potentially thousands of readers up front.
 * <p>
 * The reader itself is unique to each file type. This interface
 * provides a common interface that this framework can use to create the
 * file-specific reader on demand.
 * <p>
 * Also manages opening the reader using a scan-specific schema
 * negotiator.
 */
public interface ReaderFactory<T extends SchemaNegotiator> {
  boolean hasNext();
  ManagedReader next(T negotiator) throws EarlyEofException;
}
