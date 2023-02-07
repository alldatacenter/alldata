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
package org.apache.drill.jdbc.impl;

/**
 * Implementation of {@link net.hydromatic.avatica.AvaticaFactory}
 * for Drill and JDBC 4.0 (corresponds to JDK 1.6).
 */
// Note:  Must be public so net.hydromatic.avatica.UnregisteredDriver can
// (reflectively) call no-args constructor.
public class DrillJdbc40Factory extends DrillJdbc41Factory {

  /** Creates a factory for JDBC version 4.1. */
  // Note:  Must be public so net.hydromatic.avatica.UnregisteredDriver can
  // (reflectively) call this constructor.
  public DrillJdbc40Factory() {
    super(4, 0);
  }
}
