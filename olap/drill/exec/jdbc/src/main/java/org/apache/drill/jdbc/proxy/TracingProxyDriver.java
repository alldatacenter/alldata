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
package org.apache.drill.jdbc.proxy;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * Proxy driver for tracing calls to a JDBC driver.
 * Reports calls and parameter values to, and return values and exceptions from,
 * methods defined by JDBC interfaces.
 *
 * <p><strong>Invocation:</strong></p>
 * <p>
 *   To set up a tracing version of a JDBC connection:
 * </p>
 * <ul>
 *   <li>
 *     Construct the proxying URL corresponding to the original URL.
 *     <p>
 *       The proxying URL corresponding to an original URL is
 *       "<code>jdbc:proxy:<i>original.Driver</i>:<i>original_URL</i></code>",
 *       where:
 *     </p>
 *     <ul>
 *       <li>
 *         <code><i>original.Driver</i></code> is the fully qualified name of
 *         the driver class to proxy and trace (used to load the class to get it
 *         registered with JDBC's {@link DriverManager}, when or in case it's
 *         not already loaded) and can be blank if the driver class will already
 *         be loaded, and
 *       </li>
 *       <li>
 *         <code><i>original_URL</i></code> is the original URL for the JDBC
 *         data source to now be traced.
 *       </li>
 *     </ul>
 *     <p>
 *       For example, for an original URL of "{@code jdbc:drill:zk=local}", the
 *       tracing URL is
 *       "{@code jdbc:proxy:org.apache.drill.jdbc.Driver:jdbc:drill:zk=local}".
 *     </p>
 *   </li>
 *   <li>
 *     In the JDBC-client code or tool, replace the occurrence of the original
 *     URL with the corresponding proxying URL.
 *   </li>
 *   <li>
 *     Make sure that class {@link TracingProxyDriver} will be loaded (e.g.,
 *     configure the client to use it as the driver class).
 *   </li>
 * </ul>
 * <p><strong>Output:</strong></p>
 * <p>
 *   Currently, the tracing output lines are simply written to
 *   {@link System#out} ({@code stdout} or "standard output").
 * </p>
 */
public class TracingProxyDriver implements java.sql.Driver {

  /** JDBC URL prefix that tracing proxy driver recognizes. */
  private static final String JDBC_URL_PREFIX = "jdbc:proxy:";

  // TODO:  Maybe split into static setup reporter vs. non-static tracing
  // reporter, especially if output destination becomes configurable via
  // something in the proxy/tracing URI.
  // (Static because called in class initialization.)
  private static final InvocationReporter reporter;
  static {
    InvocationReporterImpl simpleReporter = new InvocationReporterImpl();
    // Note:  This is intended to be first line written to output:
    simpleReporter.setupMessage( "Proxy driver " + TracingProxyDriver.class
                                 + " initializing.");
    simpleReporter.reportAbbreviatedPackages();
    reporter = simpleReporter;
  }

  private final ProxiesManager proxiesManager = new ProxiesManager( reporter );

  /** Most recent (and usually only) proxyDriver created by this (usually
   *  singleton) instance. */
  private Driver proxyDriver;


  // Statically create and register an instance with JDBC's DriverManager.
  static {
    reporter.setupMessage( "Proxy driver registering with DriverManager." );
    final Driver proxyDriver;
    try {
      proxyDriver = new TracingProxyDriver();
    }
    catch ( SQLException e ) {
      throw new RuntimeException(
          "Error in initializing " + TracingProxyDriver.class + ": " + e, e );
    }
    try {
      DriverManager.registerDriver( proxyDriver );
    }
    catch ( SQLException e ) {
      throw new RuntimeException(
          "Error in registering " + TracingProxyDriver.class + ": " + e, e );
    }
  }


  public TracingProxyDriver() throws SQLException {
  }

  private static class UrlHandler {
    private static final String SYNTAX_TEXT =
        "proxy URL syntax: \"" + JDBC_URL_PREFIX + "\" + \":\" "
        + "+ optional original driver class name + \":\" "
        + "+ proxied (original) JDBC URL";

    private final String classSpec;
    private final String proxiedURL;
    private final Driver proxiedDriverForProxiedUrl;
    private final Driver proxyDriver;


    UrlHandler( ProxiesManager proxiesManager, String url )
        throws ProxySetupSQLException {

      final String variablePart = url.substring( JDBC_URL_PREFIX.length() );

      final int classEndColonPos = variablePart.indexOf( ':' );
      if ( -1 == classEndColonPos ) {
        throw new ProxySetupSQLException(
            "Connection URL syntax error: no third colon in proxy URL \"" + url
            + "\"; (" + SYNTAX_TEXT + ")" );
      }
      // Either class name (load named class) or empty string (don't load any).
      classSpec = variablePart.substring( 0, classEndColonPos );
      proxiedURL = variablePart.substring( 1 + classEndColonPos );

      if ( ! "".equals( classSpec ) ) {
        try {
          Class.forName( classSpec);
        }
        catch ( ClassNotFoundException e ) {
          throw new ProxySetupSQLException(
              "Couldn't load class \"" + classSpec + "\""
              + " (from proxy driver URL \"" + url + "\" (between second and "
              + "third colons)): " + e,
              e );
        }
      }

      try {
        reporter.setupMessage( "Proxy calling DriverManager.getDriver(...) for"
                               + " proxied URL \"" + proxiedURL + "\"." );
        proxiedDriverForProxiedUrl = DriverManager.getDriver( proxiedURL );
        reporter.setupMessage(
            "DriverManager.getDriver( \"" + proxiedURL + "\" ) returned a(n) "
            + proxiedDriverForProxiedUrl.getClass().getName() + ": "
            + proxiedDriverForProxiedUrl + "." );
      }
      catch ( SQLException e ) {
        final String message =
            "Error getting driver from DriverManager for proxied URL \""
            + proxiedURL + "\" (from proxy driver URL \"" + url + "\""
            + " (after third colon)): " + e;
        reporter.setupMessage( message );
        throw new ProxySetupSQLException( message, e );
      }
      proxyDriver =
          proxiesManager.getProxyInstanceForOriginal( proxiedDriverForProxiedUrl,
                                                      Driver.class );
    }

    public String getProxiedUrl() {
      return proxiedURL;
    }

    public Driver getProxiedDriver() {
      return proxiedDriverForProxiedUrl;
    }

    public Driver getProxyDriver() {
      return proxyDriver;
    }

  }  // class UrlHandler


  private void setProxyDriver( final Driver newProxyDriver,
                               final Driver proxiedDriver ) {
    // Note if different proxy than before.
    if ( null != this.proxyDriver && newProxyDriver != this.proxyDriver ) {
      reporter.setupMessage(
          "Note:  Multiple drivers proxied; Driver-level methods such as "
          + "getMajorVersion() will be routed to latest"
          + " (" + proxiedDriver + ")." );
    }
    this.proxyDriver = newProxyDriver;
  }

  @Override
  public boolean acceptsURL( String url ) throws SQLException {
    reporter.setupMessage( "Proxy's acceptsURL(...) called with "
                           + ( null == url ? "null" : "\"" + url + "\"." ) );
    final boolean accepted;
    if ( null == url || ! url.startsWith( JDBC_URL_PREFIX ) ) {
      accepted = false;
    }
    else {
      UrlHandler urlHandler = new UrlHandler( proxiesManager, url );
      setProxyDriver( urlHandler.getProxyDriver(), urlHandler.getProxiedDriver() );

      accepted = true;  // (If no exception in UrlHandler(...)
    }
    reporter.setupMessage(
        "Proxy's acceptsURL( " + ( null == url ? "null" : "\"" + url + "\"" )
        + " ) returning " + accepted + "." );
    return accepted;
  }

  @Override
  public Connection connect( String url, Properties info )
      throws ProxySetupSQLException {
    final Connection result;
    reporter.setupMessage( "Proxy's connect(...) called with URL "
                           + ( null == url ? "null" : "\"" + url + "\"" ) + "." );

    if ( null == url || ! url.startsWith( JDBC_URL_PREFIX ) ) {
      result = null;  // (Not a URL understood by this driver.)
    }
    else {
      UrlHandler urlHandler = new UrlHandler( proxiesManager, url );
      setProxyDriver( urlHandler.getProxyDriver(), urlHandler.getProxiedDriver() );

      // (Call connect() through proxy so it gets traced too.)
      try {
        result = proxyDriver.connect( urlHandler.getProxiedUrl(), info );
      }
      catch ( SQLException e ) {
        throw new ProxySetupSQLException( "Exception from proxied driver: " + e,
                                           e );
      }
    }
    return result;
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo( String url, Properties info )
      throws SQLException {
    return proxyDriver.getPropertyInfo( url, info );
  }

  @Override
  public int getMajorVersion() {
    return proxyDriver.getMajorVersion();
  }

  @Override
  public int getMinorVersion() {
    return proxyDriver.getMinorVersion();
  }
  @Override
  public boolean jdbcCompliant() {
    return proxyDriver.jdbcCompliant();
  }

  @Override
  public java.util.logging.Logger getParentLogger()
      throws SQLFeatureNotSupportedException {
    return proxyDriver.getParentLogger();
  }

} // class TracingProxyDriver
