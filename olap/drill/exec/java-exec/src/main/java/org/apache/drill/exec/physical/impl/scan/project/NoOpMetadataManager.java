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

import org.apache.drill.exec.physical.impl.scan.project.ScanLevelProjection.ScanProjectionParser;
import org.apache.drill.exec.physical.impl.scan.project.ReaderLevelProjection.ReaderProjectionResolver;
import org.apache.drill.exec.physical.resultSet.ResultVectorCache;

/**
 * Do-nothing implementation of the metadata manager. Allows the
 * metadata manager to be optional without needing an if-statement
 * on every access.
 */

public class NoOpMetadataManager implements MetadataManager {

  @Override
  public void bind(ResultVectorCache vectorCache) { }

  @Override
  public ScanProjectionParser projectionParser() { return null; }

  @Override
  public ReaderProjectionResolver resolver() {
    // The resolver is requested only for user-defined metadata
    // managers, not for this default, no-op version. If this
    // method is called, something is amiss with the default
    // setup.
    throw new UnsupportedOperationException("Not supported");
  }

  @Override
  public void define() { }

  @Override
  public void load(int rowCount) { }

  @Override
  public void endFile() { }

  @Override
  public void close() { }
}
