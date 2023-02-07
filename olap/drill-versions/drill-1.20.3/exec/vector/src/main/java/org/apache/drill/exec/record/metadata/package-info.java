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
 * Provides a fluent schema builder. Handles all
 * forms of Drill schemas, with emphasis on ease of use for the typical
 * cases (flat schema or nested maps.) Enables construction of unions,
 * union lists (AKA "list vector") repeated lists and combinations of
 * the above structures.
 * <dt>SchemaBuilder</dt>
 * <dd>Drill normally writes data to vectors, then "discovers" the row set schema based on the
 * data written. It is usually far easier to simply declare a schema, then
 * read and write data according to that schema. The schema builder provides a simple,
 * fluent tool to create a row set schema. That schema then drives the row set readers
 * and writers, the row set printer and the row set comparison.</dd>
 * </dl>
 */

package org.apache.drill.exec.record.metadata;
