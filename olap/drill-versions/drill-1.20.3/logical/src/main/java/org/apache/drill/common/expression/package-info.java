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
 * Logical expression tree representation.
 *
 * Drill manages expressions provided in many different parts of SQL queries.
 * This includes scalar expressions in select, filter and join conditions, as
 * well as aggregate and window functions. These expressions are represented
 * logically as ASTs during planning. The classes defined here provide the
 * different nodes in the expression tree, as well as utilities for building
 * and manipulating expressions during parsing and planning.
 *
 */
package org.apache.drill.common.expression;
