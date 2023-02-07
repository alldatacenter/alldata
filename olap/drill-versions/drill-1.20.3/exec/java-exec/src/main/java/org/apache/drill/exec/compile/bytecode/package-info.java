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
 * Bytecode manipulation utilities for stitching together runtime-generated code
 * with prebuilt templates.
 *
 * Also includes a number of performance optimizations, including scalar
 * replacement for small data-only objects to avoid object churn and
 * indirection. The object definitions are convenient for defining user-facing
 * APIs, such as Drill's UDF interface, but end up slowing down execution
 * significantly.
 */
package org.apache.drill.exec.compile.bytecode;