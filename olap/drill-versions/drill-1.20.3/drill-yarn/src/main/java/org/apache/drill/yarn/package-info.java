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
 * Hosts Apache Drill under Apache Hadoop YARN. Consists of two main
 * components as required by YARN: a client application which uses YARN to
 * start the Drill cluster, and an Application Master (AM) which manages
 * the cluster. The AM in turn starts, manages and stops drillbits.
 * <p>
 * Much of the functionality is simply plumbing to get YARN to do what is
 * needed. The core of the AM is a "cluster controller" which starts,
 * monitors and stops Drillbits, tracking their state transitions though
 * the several lifecycle stages that result.
 * <p>
 * Note about logs here: Drill-on-YARN is a YARN application and so it
 * uses the same logging system used by the YARN code. This is different
 * than that used by Drill.
 */

package org.apache.drill.yarn;