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
package org.apache.drill.exec.expr.stat;

/**
 * Define the validity of a row group against a filter
 * <ul>
 * <li>ALL : all rows match the filter (can not drop the row group and can prune the filter)
 * <li>NONE : no row matches the filter (can drop the row group)
 * <li>SOME : some rows only match the filter or the filter can not be applied (can not drop the row group nor the filter)
 * </ul>
 */
public enum RowsMatch {ALL, NONE, SOME}
