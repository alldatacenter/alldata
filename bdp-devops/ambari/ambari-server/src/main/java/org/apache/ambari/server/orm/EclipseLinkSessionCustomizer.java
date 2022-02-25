/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.orm;

import java.util.ArrayList;

import javax.activation.DataSource;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.JNDIConnector;
import org.eclipse.persistence.sessions.Session;

/**
 * The {@link EclipseLinkSessionCustomizer} is used as a way to quickly override
 * the way that EclipseLink interacts with the database. Some possible uses of
 * this class are:
 * <ul>
 * <li>Setting runtime analysis properties such as log levels and profilers</li>
 * <li>Providing a custom {@link DataSource} via {@link JNDIConnector}</li>
 * <li>Changing JDBC driver properties.</li>
 * </ul>
 * For example:
 *
 * <pre>
 * DatabaseLogin login = (DatabaseLogin) session.getDatasourceLogin();
 * ComboPooledDataSource source = new ComboPooledDataSource();
 * source.setDriverClass(login.getDriverClassName());
 * source.setMaxConnectionAge(100);
 * ...
 * login.setConnector(new JNDIConnector(source));
 *
 * <pre>
 */
public class EclipseLinkSessionCustomizer implements SessionCustomizer {

  /**
   * {@inheritDoc}
   * <p/>
   * This class exists for quick customization purposes.
   */
  @Override
  public void customize(Session session) throws Exception {
    // ensure db behavior is same as shared cache
    DatabaseLogin databaseLogin = (DatabaseLogin) session.getDatasourceLogin();
    databaseLogin.setTransactionIsolation(DatabaseLogin.TRANSACTION_READ_COMMITTED);

    // read-all queries use a Vector as their container for
    // result items - this seems like an unnecessary performance hit since
    // Vectors are synchronized and there's no apparent reason to provide a
    // thread-safe collection on a read all query
    // see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=255634
    Object ddlGeneration = session.getProperty(PersistenceUnitProperties.DDL_GENERATION);
    if (null == ddlGeneration || PersistenceUnitProperties.NONE.equals(ddlGeneration)) {
      // only set this when not using DDL generation - Sequence generation hard
      // codes Vector
      ContainerPolicy.setDefaultContainerClass(ArrayList.class);
    }
  }
}
