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
/**
 * Defines the scan operation implementation. The scan operator is a generic mechanism
 * that fits into the Drill Volcano-based iterator protocol to return record batches
 * from one or more readers.
 * <p>
 * Two versions of the scan operator exist:<ul>
 * <li>{@link ScanBatch}: the original version that uses readers based on the
 * {@link RecordReader} interface. <tt>ScanBatch</tt> cannot, however, handle
 * limited-length vectors.</li>
 * <li>{@link ScanOperatorExec}: the revised version that uses a more modular
 * design and that offers a mutator that is a bit easier to use, and can limit
 * vector sizes.</li></ul>
 * New code should use the new version, existing code will continue to use the
 * <tt>ScanBatch</tt> version until all readers are converted to the new format.
 * <p>
 * Further, the new version is designed to allow intensive unit test without
 * the need for the Drill server. New readers should exploit this feature to
 * include intensive tests to keep Drill quality high.
 * <p>
 * See {@link ScanOperatorExec} for details of the scan operator protocol
 * and components.
 *
 * <h4>Traditional Class Structure<h4>
 * The original design was simple: but required each reader to handle many
 * detailed tasks.
 * <pre><code>
 *  +------------+          +-----------+
 *  | Scan Batch |    +---> | ScanBatch |
 *  |  Creator   |    |     +-----------+
 *  +------------+    |           |
 *         |          |           |
 *         v          |           |
 *  +------------+    |           v
 *  |   Format   | ---+   +---------------+
 *  |   Plugin   | -----> | Record Reader |
 *  +------------+        +---------------+
 *
 * </code></pre>
 *
 * The scan batch creator is unique to each storage plugin and is created
 * based on the physical operator configuration ("pop config"). The
 * scan batch creator delegates to the format plugin to create both the
 * scan batch (the scan operator) and the set of readers which the scan
 * batch will manage.
 * <p>
 * The scan batch
 * provides a <code>Mutator</code> that creates the vectors used by the
 * record readers. Schema continuity comes from reusing the Mutator from one
 * file/block to the next.
 * <p>
 * One characteristic of this system is that all the record readers are
 * created up front. If we must read 1000 blocks, we'll create 1000 record
 * readers. Developers must be very careful to only allocate resources when
 * the reader is opened, and release resources when the reader is closed.
 * Else, resource bloat becomes a large problem.
 *
 * <h4>Revised Class Structure</h4>
 *
 * The new design is more complex because it divides tasks up into separate
 * classes. The class structure is larger, but each class is smaller, more
 * focused and does just one task.
 * <pre><code>
 *  +------------+          +---------------+
 *  | Scan Batch | -------> | Format Plugin |
 *  |  Creator   |          +---------------+
 *  +------------+          /        |       \
 *                         /         |        \
 *    +---------------------+        |         \ +---------------+
 *    | OperatorRecordBatch |        |     +---->| ScanFramework |
 *    +---------------------+        |     |     +---------------+
 *                                   v     |            |
 *                         +------------------+         |
 *                         | ScanOperatorExec |         |
 *                         +------------------+         v
 *                                   |            +--------------+
 *                                   +----------> | Batch Reader |
 *                                                +--------------+
 * </code></pre>
 *
 * Here, the scan batch creator again delegates to the format plugin. The
 * format plugin creates three objects:
 * <ul>
 * <li>The <code>OperatorRecordBatch</code>, which encapsulates the Volcano
 * iterator protocol. It also holds onto the output batch. This allows the
 * operator implementation to just focus on its specific job.</li>
 * <li>The <code>ScanOperatorExec</code> is the operator implementation for
 * the new result-set-loader based scan.</li>
 * <li>The scan framework is specific to each kind of reader. It handles
 * everything which is unique to that reader. Rather than inheriting from
 * the scan itself, the framework follows the strategy pattern: it says how
 * to do a scan for the target format.<li>
 * </ul>
 *
 * The overall structure uses the "composition" pattern: what is combined
 * into a small set of classes in the traditional model is broken out into
 * focused classes in the revised model.
 * <p>
 * A key part of the scan strategy is the batch reader. ("Batch" because
 * it reads an entire batch at a time, using the result set loader.) The
 * framework creates batch readers one by one as needed. Resource bloat
 * is less of an issue because only one batch reader instance exists at
 * any time for each scan operator instance.
 * <p>
 * Each of the above is further broken down into additional classes to
 * handle projection and so on.
 */

package org.apache.drill.exec.physical.impl.scan;
