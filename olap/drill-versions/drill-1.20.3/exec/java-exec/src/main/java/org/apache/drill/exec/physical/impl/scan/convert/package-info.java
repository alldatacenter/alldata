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
 * Standard type conversion tools for the case in which the input
 * types are the standard Java types already supported by the
 * {@link ValuesWriter} interface. The classes here wrap a
 * {@link ScalarWriter}: either a simple column, or the scalar portion of
 * an array column.
 * <p>
 * The converters sit outside of the column writer hierarchy. In the
 * most general case, a batch reader uses a "column adapter" to convert
 * from some source-specific input format such as a JDBC {@code ResultSet}.
 * In this case, the reader maintains a "row format": the set of column
 * adapters that make up the row.
 * <p>
 * The classes here handle a special case: when the reader just needs
 * the standard Java types. These classes allow a batch reader to intermix
 * "plain" column writers and column conversions in a "row format".
 * This in turn, ensures
 * that there is no extra function call overhead when no conversion
 * is needed.
 * <p>
 * Provides a mapping from input to output
 * (vector) type. Handles implicit conversions (those done by the column
 * writer itself) and explicit conversions for the Java string notation
 * for various types. Readers must provide custom conversions or specialized
 * formats.
 * <p>
 * Type-narrowing operations are supported by the column writers using
 * "Java semantics". Long-to-int overflow is caught, double-to-long conversion
 * sets the maximum or minimum long values, Double-to-int will overflow (on
 * the int conversion.) Messy, but the goal is not to handle invalid data,
 * rather it is to provide convenience for valid data.
 * <p>
 * The semantics of invalid conversions can be refined (set to null or
 * throw an exception) without affecting the behavior of queries with
 * valid data.
 * <p>
 * The provided conversions all handle the normal cases. Exceptional
 * case (overflow, ambiguous formats) are handled according to Java
 * rules.
 * <p>
 * The {@link StandardConversions} class defines the types of conversions
 * needed:
 * <ul>
 * <li>None: Converting from a type to itself.</li>
 * <li>Implicit: Conversion is done by the column writers, such as
 * converting from an INT to a SMALLINT.</li>
 * <li>Explicit: Requires a converter. If an unambiguous conversion is
 * possible, that converter should occur here. If conversion is ambiguous,
 * or a reader needs to support a special format, then the reader can add
 * custom conversions for these cases.</li>
 * </ul>
 * <p>
 * Would be good to validate each conversion against the corresponding CAST
 * operation. In an ideal world, all conversions, normal and exceptional,
 * will work the same as either a CAST (where the operations is handled by
 * the Project operator via code generation) and the standard conversions.
 */
package org.apache.drill.exec.physical.impl.scan.convert;

import org.apache.drill.exec.vector.accessor.ScalarWriter;
