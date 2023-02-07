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
package org.apache.drill.exec.ops;

/**
 * Indicates that an external source has cancelled the query.
 * Thrown by the operator that detects the cancellation. Bubbles
 * up the operator tree like other exceptions. However, this one
 * tells the fragment executor that the exception is one that
 * the fragment executor itself initiated and so should not
 * be reported as an error.
 */
@SuppressWarnings("serial")
public class QueryCancelledException extends RuntimeException {

  // No need for messages; this exception is silently ignored.
}
