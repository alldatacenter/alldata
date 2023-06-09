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
 * This package provides a "dummy" set of writers. The dummy writers provide
 * the same API as the "real" writers, but the dummy writers simply discard
 * their data. The dummy writers are used when implementing projection:
 * non-projected columns may still have to be processed (as in a CSV file,
 * say), but their values are not needed. One way to do this is to do an
 * if-statement for each value:<pre><code>
 * if (column-a-is-projected) {
 *   aWriter.setSomething(value);
 * }</code></pre>
 * The dummy writers convert the if-statement into a virtual function call,
 * same as is done to handle the type-specific nature of vectors:
 * <pre><code>
 * aWriter.setSomething(value);
 * </code></pre>
 * <p>
 * The theory is that the virtual function dispatch is simpler, and faster,
 * than doing continual if-checks everywhere in the code.
 * <p>
 * The dummy writers reside in this package so that the various factory
 * methods can automatically build the dummy versions when given a null
 * value vector (which we then interpret to mean that there is no physical
 * backing to the column.)
 * <p>
 * At present, most methods that return a value simply return zero or
 * null.
 * Experience will show whether it is worthwhile implementing some
 * basics, such as a value type or index. For now, these return null,
 * assuming that the caller won't do anything with the column other
 * than set a value.
 * <p>
 * Some simpler dummy writers appear as nested classes inside the
 * "real" writers.
 */

package org.apache.drill.exec.vector.accessor.writer.dummy;
