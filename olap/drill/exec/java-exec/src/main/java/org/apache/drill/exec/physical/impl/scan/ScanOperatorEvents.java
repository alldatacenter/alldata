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
package org.apache.drill.exec.physical.impl.scan;

import org.apache.drill.exec.ops.OperatorContext;

/**
 * Interface to the set of readers, and reader schema, that the scan operator
 * manages. The reader factory creates and returns the readers to use for the
 * scan, as determined by the specific physical plan. The reader factory also
 * translates from the select list provided in the physical plan to the actual
 * columns returned from the scan operator. The translation is reader-specific;
 * this interface allows the scan operator to trigger various lifecycle events.
 * <p>
 * This interface decouples the scan implementation from the generic tasks
 * needed to implement Drill's Volcano iterator protocol for operators, and
 * Drill's schema and batch semantics. A scan implementation need only
 * implement this interface to add plugin-specific scan behavior.
 * <p>
 * While this interface allows a wide variety of implementations, the intent is
 * that most actual scanners will use the "managed" framework that handles the
 * routine projection, vector management and other tasks that tend to be common
 * across scanners. See {@link ScanSchemaOrchestrator} for the managed
 * framework.
 */
public interface ScanOperatorEvents {

  /**
   * Build the scan-level schema from the physical operator select list.
   * The operator context is provided to allow access to the user name, to
   * options, and to other information that might influence schema resolution.
   * <p>
   * After this call, the schema manager should be ready to build a
   * reader-specific schema for each reader as it is opened.
   *
   * @param context the operator context for the scan operator
   */
  void bind(OperatorContext context);

  /**
   * A scanner typically readers multiple data sources (such as files or
   * file blocks.) A batch reader handles each read. This method returns
   * the next reader in whatever sequence that this scan defines.
   * <p>
   * The preferred implementation is to create each batch reader in this
   * call to minimize resource usage. Production queries may read
   * thousands of files or blocks, so incremental reader creation can be
   * far more efficient than creating readers at the start of the scan.
   *
   * @return a batch reader for one of the scan elements within the
   * scan physical plan for this scan operator
   */
  RowBatchReader nextReader();

  /**
   * Called when the scan operator itself is closed. Indicates that no more
   * readers are available.
   */
  void close();
}
