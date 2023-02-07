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
 * Implements the Drill Application Master for YARN.
 * <p>
 * Note that AM implementation classes use org.apache.commons.logging
 * to be consistent with the logging used within YARN itself. However,
 * the AM uses Drill's class path which uses logback logging. To enable
 * logging, modify
 * <code>$DRILL_HOME/conf/logback.xml</code> and add a section something
 * like this:
 * <pre><code>
 *   &lt;logger name="org.apache.drill.yarn" additivity="false">
 *    &lt;level value="trace" />
 *    &lt;appender-ref ref="STDOUT" />
 *   &lt;/logger>
 * </code></pre>
 */

package org.apache.drill.yarn.appMaster;