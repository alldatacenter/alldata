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
import org.apache.calcite.avatica.DriverVersion;
import org.apache.calcite.avatica.Handler;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.UnregisteredDriver;
import org.apache.drill.common.util.DrillVersionInfo;

/**
 * Optiq JDBC driver.
 */
public class DriverImpl extends UnregisteredDriver {
  private static final String CONNECTION_STRING_PREFIX = "jdbc:drill:";
  private static final String METADATA_PROPERTIES_RESOURCE_PATH =
      "apache-drill-jdbc.properties";


  public DriverImpl() {
    super();
  }


  @Override
  protected String getConnectStringPrefix() {
    return CONNECTION_STRING_PREFIX;
  }

  @Override
  protected String getFactoryClassName(JdbcVersion jdbcVersion) {
    switch (jdbcVersion) {
    case JDBC_30:
      return "org.apache.drill.jdbc.impl.DrillJdbc3Factory";
    case JDBC_40:
      return "org.apache.drill.jdbc.impl.DrillJdbc40Factory";
    case JDBC_41:
    default:
      return "org.apache.drill.jdbc.impl.DrillJdbc41Factory";
    }
  }

  @Override
  protected DriverVersion createDriverVersion() {
    return DriverVersion.load(
        this.getClass(),
        METADATA_PROPERTIES_RESOURCE_PATH,
        // Driver name and version:
        "Apache Drill JDBC Driver",
        DrillVersionInfo.getVersion(),
        // Database product name and version:
        "Apache Drill",
        "<Properties resource " + METADATA_PROPERTIES_RESOURCE_PATH + " not loaded>");
  }

  @Override
  public Meta createMeta(AvaticaConnection connection) {
    return new DrillMetaImpl((DrillConnectionImpl) connection);
  }

  @Override
  protected Handler createHandler() {
    return new DrillHandler();
  }

}
