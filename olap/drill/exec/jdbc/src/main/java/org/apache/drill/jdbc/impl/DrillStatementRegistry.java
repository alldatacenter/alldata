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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Registry of open statements (for a connection), for closing them when a
 * connection is closed.
 * <p>
 *   Concurrency:  Not thread-safe.  (Creating statements, closing statements,
 *   and closing connection cannot be concurrent (unless concurrency is
 *   coordinated elsewhere).)
 * </p>
 */
class DrillStatementRegistry {

  private static final Logger logger = getLogger( DrillStatementRegistry.class );

  /** ... (using as IdentityHash*Set*) */
  private final Map<Statement, Object> openStatements = new IdentityHashMap<>();


  void addStatement( Statement statement ) {
    logger.debug( "Adding to open-statements registry: " + statement );
    openStatements.put( statement, statement );
  }

  void removeStatement( Statement statement ) {
    logger.debug( "Removing from open-statements registry: " + statement );
    openStatements.remove( statement );
  }

  void close() {
    // Note:  Can't call close() on statement during iteration of map because
    // close() calls our removeStatement(...), which modifies the map.

    // Copy set of open statements to other collection before closing:
    final List<Statement> copiedList = new ArrayList<>( openStatements.keySet() );

    for ( final Statement statement : copiedList ) {
      try {
        logger.debug( "Auto-closing (via open-statements registry): " + statement );
        statement.close();
      }
      catch ( SQLException e ) {
        logger.error( "Error auto-closing statement " + statement + ": " + e, e );
        // Otherwise ignore the error, to close which statements we can close.
      }
    }
    openStatements.clear();
  }
}
