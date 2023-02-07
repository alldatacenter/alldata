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

import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.AvaticaFactory;
import org.apache.calcite.avatica.UnregisteredDriver;

import java.sql.SQLException;
import java.util.Properties;

/**
 * Partial implementation of {@link AvaticaFactory}
 * (factory for main JDBC objects) for Drill's JDBC driver.
 * <p>
 *   Handles JDBC version number.
 * </p>
 */
abstract class DrillFactory implements AvaticaFactory {
  protected final int major;
  protected final int minor;

  /** Creates a JDBC factory with given major/minor version number. */
  protected DrillFactory(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  @Override
  public int getJdbcMajorVersion() {
    return major;
  }

  @Override
  public int getJdbcMinorVersion() {
    return minor;
  }


  /**
   * Creates a Drill connection for Avatica (in terms of Avatica types).
   * <p>
   *   This implementation delegates to
   *   {@link #newDrillConnection(DriverImpl, DrillFactory, String, Properties)}.
   * </p>
   */
  @Override
  public final AvaticaConnection newConnection(UnregisteredDriver driver,
                                               AvaticaFactory factory,
                                               String url,
                                               Properties info) throws SQLException {
    return newDrillConnection((DriverImpl) driver, (DrillFactory) factory, url, info);
  }

  /**
   * Creates a Drill connection (in terms of Drill-specific types).
   */
  abstract DrillConnectionImpl newDrillConnection(DriverImpl driver,
                                                  DrillFactory factory,
                                                  String url,
                                                  Properties info) throws SQLException;
}
