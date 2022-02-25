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
package org.apache.ambari.server.notifications.dispatchers;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AlertNotification;
import org.apache.ambari.server.state.alert.TargetType;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService.AlertInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.inject.Inject;

/**
 * The {@link AlertScriptDispatcher} is used to dispatch
 * {@link AlertNotification}s to a script via a {@link Process} and command line
 * arguments. This dispatcher does not know how to work with any other types of
 * {@link Notification}.
 * <p/>
 * This dispatcher only deals with non-digest notifications.
 */
public class AlertScriptDispatcher implements NotificationDispatcher {

  /**
   * The key in {@code ambari.properties} which contains the script to execute.
   */
  public static final String SCRIPT_CONFIG_DEFAULT_KEY = "notification.dispatch.alert.script";

  /**
   * The key in {@code ambari.properties} which contains the timeout, in
   * milliseconds, of any script run by this dispatcher.
   */
  public static final String SCRIPT_CONFIG_TIMEOUT_KEY = "notification.dispatch.alert.script.timeout";

  /**
   * A dispatch property that instructs this dispatcher to read a different key
   * from {@link Configuration}. If this value is not a part of the
   * {@link Notification#DispatchProperties} then
   * {@link #SCRIPT_CONFIG_DEFAULT_KEY} will be used.
   */
  public static final String DISPATCH_PROPERTY_SCRIPT_CONFIG_KEY = "ambari.dispatch-property.script";

  /**
   * A dispatch property that instructs this dispatcher to lookup script by filename
   * from {@link org.apache.ambari.server.state.alert.AlertTarget}.
   */
  public static final String DISPATCH_PROPERTY_SCRIPT_FILENAME_KEY  = "ambari.dispatch-property.script.filename";


  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AlertScriptDispatcher.class);

  /**
   * Default script timeout is 5s
   */
  private static final long DEFAULT_SCRIPT_TIMEOUT = 5000L;

  /**
   * Used to escape text being passed into the shell command.
   */
  public static final Escaper SHELL_ESCAPE;

  static {
    final Escapers.Builder builder = Escapers.builder();
    builder.addEscape('\"', "\\\"");
    builder.addEscape('!', "\\!");
    SHELL_ESCAPE = builder.build();
  }

  /**
   * Configuration data from the ambari.properties file.
   */
  @Inject
  protected Configuration m_configuration;

  /**
   * The executor responsible for dispatching.
   */
  private final Executor m_executor = new ThreadPoolExecutor(0, 1, 5L, TimeUnit.MINUTES,
    new LinkedBlockingQueue<>(), new ScriptDispatchThreadFactory(),
      new ThreadPoolExecutor.CallerRunsPolicy());

  /**
   * Gets the key that will be used to lookup the script to execute from
   * {@link Configuration}.
   *
   * @return the key that will be used to lookup the script value from
   *         {@link Configuration} (never {@code null}).
   */
  public String getScriptConfigurationKey( Notification notification ) {
    if( null == notification || null == notification.DispatchProperties ){
      return SCRIPT_CONFIG_DEFAULT_KEY;
    }

    if (null == notification.DispatchProperties.get(DISPATCH_PROPERTY_SCRIPT_CONFIG_KEY)) {
      return SCRIPT_CONFIG_DEFAULT_KEY;
    }

    return notification.DispatchProperties.get(DISPATCH_PROPERTY_SCRIPT_CONFIG_KEY);
  }

  /**
   * Gets the timeout value for the script, in milliseconds, as set in
   * {@link Configuration}. If there is no setting, then
   * {@link #DEFAULT_SCRIPT_TIMEOUT} will be returned.
   *
   * @return the timeout value in milliseconds
   */
  public long getScriptConfigurationTimeout(){
    String scriptTimeout = m_configuration.getProperty(SCRIPT_CONFIG_TIMEOUT_KEY);
    if (null == scriptTimeout) {
      return DEFAULT_SCRIPT_TIMEOUT;
    }

    return Long.parseLong(scriptTimeout);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getType() {
    return TargetType.ALERT_SCRIPT.name();
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Returns {@code false} always.
   */
  @Override
  public final boolean isNotificationContentGenerationRequired() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatch(Notification notification) {
    String scriptKey = null;
    String script = getScriptLocation(notification);

    if( null == script){ // Script filename is null.
        scriptKey = getScriptConfigurationKey(notification);
        script = m_configuration.getProperty(scriptKey);
    }

    // this dispatcher requires a script to run
    if (null == script) {
      LOG.warn(
          "Unable to dispatch notification because the {} configuration property was not found",
          scriptKey);

      if (null != notification.Callback) {
        notification.Callback.onFailure(notification.CallbackIds);
      }

      return;
    }

    // this dispatcher can only handler alert notifications
    if (notification.getType() != Notification.Type.ALERT) {
      LOG.warn("The {} dispatcher is not able to dispatch notifications of type {}", getType(),
          notification.getType());

      if (null != notification.Callback) {
        notification.Callback.onFailure(notification.CallbackIds);
      }

      return;
    }

    // execute the script asynchronously
    long timeout = getScriptConfigurationTimeout();
    TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    AlertNotification alertNotification = (AlertNotification) notification;
    ProcessBuilder processBuilder = getProcessBuilder(script, alertNotification);

    AlertScriptRunnable runnable = new AlertScriptRunnable(alertNotification, script,
        processBuilder,
        timeout, timeUnit);

    m_executor.execute(runnable);
  }

  /**
   * Gets the dispatch script location from ambari.properties and notification.
   *
   * @param notification
   * @return the dispatch script location.If script filename is {@code null},
   *         {@code null} will be returned.
   */

  String getScriptLocation(Notification notification){
    String scriptName = null;
    String scriptDir = null;

    if( null == notification || null == notification.DispatchProperties )
        return null;

    scriptName = notification.DispatchProperties.get(DISPATCH_PROPERTY_SCRIPT_FILENAME_KEY);
    if( null == scriptName) {
        LOG.warn("the {} configuration property was not found for dispatching notification",
                DISPATCH_PROPERTY_SCRIPT_FILENAME_KEY);
        return null;
    }

    scriptDir = m_configuration.getDispatchScriptDirectory();

    return scriptDir + File.separator + scriptName;
  }


  /**
   * {@inheritDoc}
   * <p/>
   * Returns {@code false} always.
   */
  @Override
  public final boolean isDigestSupported() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final TargetConfigurationResult validateTargetConfig(Map<String, Object> properties) {

    // there's no setup required for this dispatcher; always return valid
    return TargetConfigurationResult.valid();
  }

  /**
   * Gets a {@link ProcessBuilder} initialized with a script command to run with
   * the parameters from the notification.
   *
   * @param script
   *          the absolute path to the script (not {@code null}).
   * @param notification
   *          the notification to parameterie (not {@code null}).
   * @return
   */
  ProcessBuilder getProcessBuilder(String script, AlertNotification notification) {
    final String shellCommand;
    final String shellCommandOption;
    if (SystemUtils.IS_OS_WINDOWS) {
      shellCommand = "cmd";
      shellCommandOption = "/c";
    } else {
      shellCommand = "sh";
      shellCommandOption = "-c";
    }

    AlertInfo alertInfo = notification.getAlertInfo();
    AlertDefinitionEntity definition = alertInfo.getAlertDefinition();
    String definitionName = definition.getDefinitionName();
    AlertState alertState = alertInfo.getAlertState();
    String serviceName = alertInfo.getServiceName();

    // these could have spaces in them, so quote them so they don't mess up the
    // command line
    String alertLabel = "\"" + SHELL_ESCAPE.escape(definition.getLabel()) + "\"";
    String alertText = "\"" + SHELL_ESCAPE.escape(alertInfo.getAlertText()) + "\"";

    long alertTimestamp = alertInfo.getAlertTimestamp();
    String hostName = alertInfo.getHostName(); // null if alert do not run against host

    Object[] params = new Object[] { script, definitionName, alertLabel, serviceName,
        alertState.name(), alertText, alertTimestamp, hostName};

    String foo = StringUtils.join(params, " ");

    // sh -c '/foo/sys_logger.py ambari_server_agent_heartbeat "Agent Heartbeat"
    // AMBARI CRITICAL "Something went wrong with the host" 1111111 host222'
    return new ProcessBuilder(shellCommand, shellCommandOption, foo);
  }

  /**
   * The {@link AlertScriptRunnable} is used to invoke a script with alert data
   * inside of an {@link Executor}.
   */
  private final static class AlertScriptRunnable implements Runnable {
    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AlertScriptRunnable.class);

    private final ProcessBuilder m_processBuilder;
    private final long m_timeout;
    private final TimeUnit m_timeoutUnits;
    private final Notification m_notification;
    private final String m_script;

    /**
     * Constructor.
     *
     * @param notification
     * @param script
     * @param processBuilder
     * @param timeout
     * @param timeoutUnits
     */
    private AlertScriptRunnable(Notification notification, String script,
        ProcessBuilder processBuilder,
        long timeout, TimeUnit timeoutUnits) {
      m_notification = notification;
      m_script = script;
      m_processBuilder = processBuilder;
      m_timeout = timeout;
      m_timeoutUnits = timeoutUnits;
    }

    /**
     *
     */
    @Override
    public void run() {
      boolean isDispatchSuccessful = true;

      try {
        Process process = m_processBuilder.start();
        int exitCode = execute(process, m_timeout, TimeUnit.MILLISECONDS);

        if (exitCode != 0) {
          LOG.warn("Unable to dispatch {} notification because {} terminated with exit code {}",
              TargetType.ALERT_SCRIPT, m_script, exitCode);

          isDispatchSuccessful = false;
        }
      } catch (TimeoutException timeoutException) {
        isDispatchSuccessful = false;

        LOG.warn("Unable to dispatch notification with {} in under {}ms", m_script,
            m_timeoutUnits.toMillis(m_timeout), timeoutException);
      } catch (Exception exception) {
        isDispatchSuccessful = false;

        LOG.warn("Unable to dispatch notification with {}", m_script, exception);
      }

      // callback
      if (null != m_notification.Callback) {
        if (isDispatchSuccessful) {
          m_notification.Callback.onSuccess(m_notification.CallbackIds);
        } else {
          m_notification.Callback.onFailure(m_notification.CallbackIds);
        }
      }
    }

    /**
     * Executes the specified process within the supplied timeout value. If the
     * process terminates normally within the timeout period, then the exit code
     * is returned. If the process did not succeed within the specified timeout
     * value, then a {@link TimeoutException} is thrown.
     * <p/>
     * If the process did not finish within the specified timeout, then the
     * process will be destroyed via {@link Process#destroy()}.
     *
     * @param process
     *          the process to execute (not {@code null}).
     * @param timeout
     *          the timeout to apply to the process.
     * @param unit
     *          the time units for the timeout
     * @return the exit code of the process
     * @throws TimeoutException
     *           if the process did not finish within the specified time.
     */
    public int execute(Process process, long timeout, TimeUnit unit) throws TimeoutException,
        InterruptedException {
      long timeRemaining = unit.toMillis(timeout);
      long startTime = System.currentTimeMillis();

      while (timeRemaining > 0) {
        try {
          return process.exitValue();
        } catch (IllegalThreadStateException ex) {
          Thread.sleep(Math.min(timeRemaining, 500));
        }

        long timeElapsed = System.currentTimeMillis() - startTime;
        timeRemaining = unit.toMillis(timeout) - timeElapsed;
      }

      process.destroy();

      throw new TimeoutException();
    }
  }

  /**
   * A custom {@link ThreadFactory} for the threads that will handle dispatching
   * to scripts. Threads created will have slightly reduced priority since
   * {@link AlertNotification} instances are not critical to the system.
   */
  private static final class ScriptDispatchThreadFactory implements ThreadFactory {

    private static final AtomicInteger s_threadIdPool = new AtomicInteger(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r, "script-dispatcher-" + s_threadIdPool.getAndIncrement());

      thread.setDaemon(false);
      thread.setPriority(Thread.NORM_PRIORITY - 1);

      return thread;
    }
  }
}
