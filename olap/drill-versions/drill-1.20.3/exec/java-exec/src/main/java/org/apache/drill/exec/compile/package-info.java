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
 * Runtime code-generation, compilation and bytecode-manipulation utilities.
 *
 * To achieve optimal performance in a dynamic schema environment, Drill
 * includes a complete code generation system for generating type-specific Java
 * code for most of its physical operators. This generated code is used for
 * scalar expression evaluation as well as value transfers between record
 * batches. As a new schema arrives at an operator, the schema change will be
 * detected and prompt runtime code generation. This runtime-generated code will
 * evaluate the expressions needed for the operator to process the incoming
 * data. As the same code will often be run on a number of nodes in the cluster,
 * compiled classes are stored in the distributed cache to avoid the need to re-
 * compile and JIT-optimize the same code on every machine.
 */
package org.apache.drill.exec.compile;