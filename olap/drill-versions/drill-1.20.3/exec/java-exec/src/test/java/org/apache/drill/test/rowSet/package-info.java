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
 * Provides a set of tools to work with row sets when creating operator
 * and "sub-operator" unit tests. A row set is a batch of Drill vectors,
 * often called a "record batch." However, a record batch, in Drill, means
 * not just the data, but also an operator on that data. The classes
 * here work with the data itself, and can be used to test implementations
 * of things such as code generated classes and so on.
 * <p>
 * Drill defines a variety of record batch semantics, modeled here as
 * distinct row set classes:
 * <dl>
 * <dt>RowSetComparison</dt>
 * <dd>Used in tests to compare an "actual" row set against an "expected"
 * row set. Does a complete check of row counts, types and values. If values
 * are arrays (repeated), does a check of the entire array. Uses JUnit assertions
 * to report comparison failures.</dd>
 */

package org.apache.drill.test.rowSet;
