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

package org.apache.ambari.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.kerberos.client.KdcConfig;
import org.apache.directory.kerberos.client.KdcConnection;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Utility class which checks connection to Kerberos Server.
 * <p>
 * It has two potential clients.
 * <ul>
 * <li>Ambari Agent:
 * Uses it to make sure host can talk to specified KDC Server
 * </li>
 * <p/>
 * <li>Ambari Server:
 * Uses it for connection check, like agent, and also validates
 * the credentials provided on Server side.
 * </li>
 * </ul>
 * </p>
 */
@Singleton
public class KdcServerConnectionVerification {

  private static final Logger LOG = LoggerFactory.getLogger(KdcServerConnectionVerification.class);

  private Configuration config;

  /**
   * The connection timeout in seconds.
   */
  private int connectionTimeout = 10;

  @Inject
  public KdcServerConnectionVerification(Configuration config) {
    this.config = config;
  }


  /**
   * Given server IP or hostname, checks if server is reachable i.e.
   * we can make a socket connection to it. Hostname may contain port
   * number separated by a colon.
   *
   * @param kdcHost KDC server IP or hostname (with optional port number)
   * @return true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String kdcHost) {
    try {
      if (kdcHost == null || kdcHost.isEmpty()) {
        throw new IllegalArgumentException("Invalid hostname for KDC server");
      }
      String[] kdcDetails = kdcHost.split(":");
      if (kdcDetails.length == 1) {
        return isKdcReachable(kdcDetails[0], parsePort(config.getDefaultKdcPort()));
      } else {
        return isKdcReachable(kdcDetails[0], parsePort(kdcDetails[1]));
      }
    } catch (Exception e) {
      LOG.error("Exception while checking KDC reachability: " + e);
      return false;
    }
  }

  /**
   * Given a host and port, checks if server is reachable meaning that we
   * can communicate with it.  First we attempt to connect via TCP and if
   * that is unsuccessful, attempt via UDP. It is important to understand that
   * we are not validating credentials, only attempting to communicate with server
   * process for the give host and port.
   *
   * @param server KDC server IP or hostname
   * @param port   KDC port
   * @return true, if server is accepting connection given port; false otherwise.
   */
  public boolean isKdcReachable(String server, int port) {
    boolean success = isKdcReachable(server, port, ConnectionProtocol.TCP) || isKdcReachable(server, port, ConnectionProtocol.UDP);

    if (!success) {
      LOG.error("Failed to connect to the KDC at {}:{} using either TCP or UDP", server, port);
    }

    return success;
  }

  /**
   * Attempt to communicate with KDC server over a specified communication protocol (TCP or UDP).
   *
   * @param server         KDC hostname or IP address
   * @param port           KDC server port
   * @param connectionProtocol the type of connection to use
   * @return true if communication is successful; false otherwise
   */
  public boolean isKdcReachable(final String server, final int port, final ConnectionProtocol connectionProtocol) {
    int timeoutMillis = connectionTimeout * 1000;
    final KdcConfig config = KdcConfig.getDefaultConfig();
    config.setHostName(server);
    config.setKdcPort(port);
    config.setUseUdp(ConnectionProtocol.UDP == connectionProtocol);
    config.setTimeout(timeoutMillis);

    FutureTask<Boolean> future = new FutureTask<>(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        Boolean success;

        try {
          KdcConnection connection = getKdcConnection(config);
          // we are only testing whether we can communicate with server and not
          // validating credentials
          connection.getTgt("noUser@noRealm", "noPassword");

          LOG.info(String.format("Encountered no Exceptions while testing connectivity to the KDC:\n" +
              "**** Host: %s:%d (%s)",
            server, port, connectionProtocol.name()));
          success = true;
        } catch (KerberosException e) {
          KrbError error = e.getError();
          ErrorType errorCode = error.getErrorCode();

          String errorCodeMessage;
          int errorCodeCode;
          if (errorCode != null) {
            errorCodeMessage = errorCode.getMessage();
            errorCodeCode = errorCode.getValue();
          } else {
            errorCodeMessage = "<Not Specified>";
            errorCodeCode = -1;
          }

          // unfortunately, need to look at msg as error 60 is a generic error code
          //todo: evaluate other error codes to provide better information
          //todo: as there may be other error codes where we should return false
          success = !(errorCodeCode == ErrorType.KRB_ERR_GENERIC.getValue() &&
            errorCodeMessage.contains("TimeOut"));

          if (!success || LOG.isDebugEnabled()) {
            KerberosMessageType messageType = error.getMessageType();

            String messageTypeMessage;
            int messageTypeCode;
            if (messageType != null) {
              messageTypeMessage = messageType.getMessage();
              messageTypeCode = messageType.getValue();
            } else {
              messageTypeMessage = "<Not Specified>";
              messageTypeCode = -1;
            }

            String message = String.format("Received KerberosException while testing connectivity to the KDC: %s\n" +
                "**** Host:    %s:%d (%s)\n" +
                "**** Error:   %s\n" +
                "**** Code:    %d (%s)\n" +
                "**** Message: %d (%s)",
              e.getLocalizedMessage(), server, port, connectionProtocol.name(), error.getEText(), errorCodeCode,
              errorCodeMessage, messageTypeCode, messageTypeMessage);

            if (LOG.isDebugEnabled()) {
              LOG.info(message, e);
            } else {
              LOG.info(message);
            }
          }
        } catch (Throwable e) {
          LOG.info(String.format("Received Exception while testing connectivity to the KDC: %s\n**** Host: %s:%d (%s)",
            e.getLocalizedMessage(), server, port, connectionProtocol.name()), e);

          // some bad unexpected thing occurred
          throw new RuntimeException(e);
        }

        return success;
      }
    });

    new Thread(future, "ambari-kdc-verify").start();
    Boolean result;
    try {
      // timeout after specified timeout
      result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);

      if (result) {
        LOG.info(String.format("Successfully connected to the KDC server at %s:%d over %s",
            server, port, connectionProtocol.name()));
      } else {
        LOG.warn(String.format("Failed to connect to the KDC server at %s:%d over %s",
            server, port, connectionProtocol.name()));
      }
    } catch (InterruptedException e) {
      String message = String.format("Interrupted while trying to communicate with KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
      future.cancel(true);
    } catch (ExecutionException e) {
      String message = String.format("An unexpected exception occurred while attempting to communicate with the KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
    } catch (TimeoutException e) {
      String message = String.format("Timeout occurred while attempting to to communicate with KDC server at %s:%d over %s",
          server, port, connectionProtocol.name());
      if (LOG.isDebugEnabled()) {
        LOG.warn(message, e);
      } else {
        LOG.warn(message);
      }

      result = false;
      future.cancel(true);
    }

    return result;
  }

  /**
   * Get a KDC UDP connection for the given configuration.
   * This has been extracted into it's own method primarily
   * for unit testing purposes.
   *
   * @param config KDC connection configuration
   * @return new KDC connection
   */
  protected KdcConnection getKdcConnection(KdcConfig config) {
    return new KdcConnection(config);
  }

  /**
   * Set the connection timeout.
   * This is the amount of time that we will attempt to read data from connection.
   *
   * @param timeoutSeconds timeout in seconds
   */
  public void setConnectionTimeout(int timeoutSeconds) {
    connectionTimeout = (timeoutSeconds < 1) ? 1 : timeoutSeconds;
  }

  /**
   * Get the timeout value.
   *
   * @return the connection timeout value in seconds
   */
  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Parses port number from given string.
   *
   * @param port port number string
   * @return parsed port number
   * @throws NumberFormatException    if given string cannot be parsed
   * @throws IllegalArgumentException if given string is null or empty
   */
  protected int parsePort(String port) {
    if (StringUtils.isEmpty(port)) {
      throw new IllegalArgumentException("Port number must be non-empty, non-null positive integer");
    }
    return Integer.parseInt(port);
  }

  /**
   * A connection protocol to use to for connecting to the KDC
   */
  public enum ConnectionProtocol {
    TCP,
    UDP
  }
}
