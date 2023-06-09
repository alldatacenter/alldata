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
 * Implementation of a row set model for hyper-batches. A hyper batch is
 * one that contains a list of batches. The batch is logically comprised
 * of "hyper-vectors" which are the individual vectors from each batch
 * stacked "end-to-end."
 * <p>
 * Hyper batches allow only reading. So, the only services here are to
 * parse a hyper-container into a row set model, then use that model to
 * create a matching set of readers.
 */

package org.apache.drill.exec.physical.resultSet.model.hyper;