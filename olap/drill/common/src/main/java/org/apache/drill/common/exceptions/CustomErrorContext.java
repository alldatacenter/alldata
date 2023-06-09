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
package org.apache.drill.common.exceptions;

/**
 * Generic mechanism to pass error context throughout the row set
 * mechanism and scan framework. The idea is to encapsulate error
 * context information in an object that can be passed around, rather
 * than having to pass the error information directly. In many cases, the
 * same mechanisms are called from multiple contexts, making it hard to
 * generalize the information that would be required. By hiding that
 * information in an error context, the caller decides what to add to
 * the error, the intermediate classes just pass along this opaque
 * context.
 * <p>
 * In some cases, such as file scans within a scan operator, there can be
 * multiple levels of context. A format plugin, say, can describe the
 * plugin and any interesting options. Then, a file scan can create a child
 * context that adds things like file name, split offset, etc.
 * <p>
 * If this proves useful elsewhere, it can be moved into the same
 * package as UserError, and a new <tt>addContext()</tt> method added
 * to the <tt>UserException.Builder</tt> to make the error context
 * easier to use.
 */

public interface CustomErrorContext {
  void addContext(UserException.Builder builder);
}
