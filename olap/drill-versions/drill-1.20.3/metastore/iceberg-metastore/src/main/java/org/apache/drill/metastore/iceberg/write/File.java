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
package org.apache.drill.metastore.iceberg.write;

import org.apache.iceberg.Metrics;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;

/**
 * Written file information holder.
 * Provides input / output file instances, file location and its metrics.
 */
public class File {

  private final OutputFile outputFile;
  private final Metrics metrics;

  public File(OutputFile outputFile, Metrics metrics) {
    this.outputFile = outputFile;
    this.metrics = metrics;
  }

  public OutputFile output() {
    return outputFile;
  }

  public InputFile input() {
    return outputFile.toInputFile();
  }

  public Metrics metrics() {
    return metrics;
  }

  public String location() {
    return outputFile.location();
  }
}
