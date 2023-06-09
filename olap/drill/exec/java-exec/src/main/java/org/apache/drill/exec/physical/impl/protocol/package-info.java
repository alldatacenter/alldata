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
 * Defines a revised implementation of the Drill RecordBatch protocol. This
 * version separates concerns into specific classes, and creates as single
 * "shim" class to implement the iterator protocol, deferring to specific
 * classes as needed.
 * <p>
 * This version is an eventual successor to the original implementation which
 * used the "kitchen sink" pattern to combine all functionality into s single,
 * large record batch implementation.
 */

package org.apache.drill.exec.physical.impl.protocol;