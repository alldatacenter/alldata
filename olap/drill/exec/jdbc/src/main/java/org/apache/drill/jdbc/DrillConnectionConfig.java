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
package org.apache.drill.jdbc;

import java.util.Properties;
import java.util.TimeZone;

import org.apache.calcite.avatica.ConnectionConfigImpl;


// TODO(DRILL-3730):  Change public DrillConnectionConfig from class to
// interface.  Move implementation (including inheritance from
// net.hydromatic.avatica.ConnectionConfigImpl) from published-interface package
// org.apache.drill.jdbc to class in implementation package
// org.apache.drill.jdbc.impl.
/**
 *  <p>
 *    NOTE:  DrillConnectionConfig will be changed from a class to an interface.
 *  </p>
 *  <p>
 *    In the meantime, clients must not use the fact that
 *    {@code DrillConnectionConfig} currently extends
 *    {@link ConnectionConfigImpl}.  They must call only
 *    methods declared directly in DrillConnectionConfig (or inherited Object).
 *  </p>
 */
public class DrillConnectionConfig extends ConnectionConfigImpl {
  private final Properties props;

  public DrillConnectionConfig(Properties p){
    super(p);
    this.props = p;
  }

  public boolean isLocal(){
    // TODO  Why doesn't this call getZookeeperConnectionString()?
    return "local".equals(props.getProperty("zk"));
  }

  // True if the URL points directly to a drillbit
  public boolean isDirect(){
    return props.getProperty("drillbit")!=null;
  }

  // TODO: Check: Shouldn't something validate that URL has "zk" parameter?
  public String getZookeeperConnectionString(){
    return props.getProperty("zk");
  }

  public TimeZone getTimeZone(){
    return TimeZone.getDefault();
  }

  public boolean isServerPreparedStatementDisabled() {
    return Boolean.valueOf(props.getProperty("server.preparedstatement.disabled"));
  }

  public boolean isServerMetadataDisabled() {
    return Boolean.valueOf(props.getProperty("server.metadata.disabled"));
  }
}
