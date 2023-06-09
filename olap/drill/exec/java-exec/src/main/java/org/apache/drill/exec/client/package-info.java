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
 * Java client API for submitting queries and accepting result sets from a Drill
 * server.
 *
 * Data returned to this client is stored in the default columnar Value Vector
 * data structures used by Drill internally. For users of Drill requiring a more
 * traditional database interface to consume from an application, a JDBC driver
 * is available as well; see the {@see org.apache.drill.jdbc} package.
 */
package org.apache.drill.exec.client;