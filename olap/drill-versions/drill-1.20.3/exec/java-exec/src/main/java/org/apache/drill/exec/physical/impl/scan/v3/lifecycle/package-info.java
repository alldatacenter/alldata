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
 * Implements the details of the scan lifecycle for a set of readers,
 * primarily the process of resolving the scan output schema from a variety
 * of input schemas, then running each reader, each of which will produce
 * some number of batches. Handles missing and implicit columns.
 * <p>
 * Defines the projection, vector continuity and other operations for
 * a set of one or more readers. Separates the core reader protocol from
 * the logic of working with batches.
 *
 * <h4>Schema Evolution</h4>
 *
 * Drill discovers schema on the fly. The scan operator hosts multiple readers.
 * In general, each reader may have a distinct schema, though the user typically
 * arranges data in a way that scanned files have a common schema (else SQL
 * is the wrong tool for analysis.) Still, subtle changes can occur: file A
 * is an old version without a new column c, while file B includes the column.
 * And so on.
 * <p>
 * The scan operator resolves the scan schema, striving to send a single, uniform
 * schema downstream. That schema should represent the data from all readers
 * in this scan and in other fragments of the same logical scan. The difficulty
 * arises when the information available underdetermines the output schema:
 * the mechanism here attempts to fill in gaps, and flags conflicts. Only a
 * provided or defined schema (see {@link ScanSchemaTracker} resolves all
 * ambiguities.)
 */
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;
