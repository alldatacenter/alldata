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
 * Implements a "YARN client" for Drill-on-YARN. The client uploads files to
 * DFS, then requests that YARN start the Application Master. Much fiddling
 * about is required to support this, such as zipping up the user's configuration,
 * creating a local file with the app id so we can get app status and shut down
 * the app, etc.
 * <p>
 * Divided into a main program ({@link DrillOnYarn}) and a series of commands.
 * Some commands are further divided into tasks. Builds on the
 * YARN and DFS facades defined in the core module.
 */

package org.apache.drill.yarn.client;