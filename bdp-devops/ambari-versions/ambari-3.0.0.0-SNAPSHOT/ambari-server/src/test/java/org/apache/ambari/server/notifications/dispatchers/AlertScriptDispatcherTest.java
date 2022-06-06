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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.AlertNotification;
import org.apache.ambari.server.state.alert.TargetType;
import org.apache.ambari.server.state.services.AlertNoticeDispatchService.AlertInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests {@link AlertScriptDispatcher}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ProcessBuilder.class, AlertScriptDispatcher.class })
public class AlertScriptDispatcherTest {

  private static final String SCRIPT_CONFIG_VALUE = "/foo/script.py";

  private static final String DISPATCH_PROPERTY_SCRIPT_DIRECTORY_KEY = "notification.dispatch.alert.script.directory";

  private Injector m_injector;

  @Inject
  private DispatchFactory m_dispatchFactory;

  @Inject
  private Configuration m_configuration;

  @Before
  public void before() throws Exception {
    m_injector = Guice.createInjector(new MockModule());
    m_injector.injectMembers(this);
  }

  /**
   * Tests that a callback error happens when the notification is not an
   * {@link AlertNotification}.
   */
  @Test
  public void testNonAlertNotification() throws Exception {
    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    Notification notification = EasyMock.createNiceMock(Notification.class);
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    callback.onFailure(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    EasyMock.replay(callback, notification);

    NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    dispatcher.dispatch(notification);

    EasyMock.verify(callback, notification);
  }

  /**
   * Tests that a missing script property results in a callback failure.
   */
  @Test
  public void testMissingScriptConfiguration() throws Exception {
    m_configuration.setProperty(AlertScriptDispatcher.SCRIPT_CONFIG_DEFAULT_KEY, null);

    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    callback.onFailure(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    EasyMock.replay(callback);

    NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    dispatcher.dispatch(notification);

    EasyMock.verify(callback);
  }

  /**
   * Tests a successfull invocation of the script.
   *
   * @throws Exception
   */
  @Test
  public void testProcessBuilderInvocation() throws Exception {
    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    callback.onSuccess(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    AlertScriptDispatcher dispatcher = (AlertScriptDispatcher) m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    m_injector.injectMembers(dispatcher);

    ProcessBuilder powerMockProcessBuilder = m_injector.getInstance(ProcessBuilder.class);
    EasyMock.expect(dispatcher.getProcessBuilder(SCRIPT_CONFIG_VALUE, notification)).andReturn(
        powerMockProcessBuilder).once();

    EasyMock.replay(callback, dispatcher);

    dispatcher.dispatch(notification);

    EasyMock.verify(callback, dispatcher);
    PowerMock.verifyAll();
  }

  /**
   * Tests that we will pickup the correct script when its specified on the
   * notification.
   */
  @Test
  public void testCustomScriptConfiguration() throws Exception {
    // remove the default value from configuration and put the customized value
    final String customScriptKey = "foo.bar.key";
    final String customScriptValue = "/foo/bar/foobar.py";
    m_configuration.setProperty(AlertScriptDispatcher.SCRIPT_CONFIG_DEFAULT_KEY, null);
    m_configuration.setProperty(customScriptKey, customScriptValue);

    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    // put the custom config value
    notification.DispatchProperties = new HashMap<>();
    notification.DispatchProperties.put(AlertScriptDispatcher.DISPATCH_PROPERTY_SCRIPT_CONFIG_KEY,
        customScriptKey);

    callback.onSuccess(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    AlertScriptDispatcher dispatcher = (AlertScriptDispatcher) m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    m_injector.injectMembers(dispatcher);

    ProcessBuilder powerMockProcessBuilder = m_injector.getInstance(ProcessBuilder.class);
    EasyMock.expect(dispatcher.getProcessBuilder(customScriptValue, notification)).andReturn(
        powerMockProcessBuilder).once();

    EasyMock.replay(callback, dispatcher);

    dispatcher.dispatch(notification);

    EasyMock.verify(callback, dispatcher);
    PowerMock.verifyAll();
  }

  /**
   * Tests the invocation of method getScriptLocation().
   */
  @Test
  public void testGetScriptLocation() throws Exception {
    AlertScriptDispatcher dispatcher = (AlertScriptDispatcher) m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    m_injector.injectMembers(dispatcher);

    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());
    notification.DispatchProperties = new HashMap();

    //1.ambari.dispatch-property.script.filename is not set in notification
    Assert.assertEquals(dispatcher.getScriptLocation(notification),null);

    //2.ambari.dispatch-property.script.filename is set in notification,but notification.dispatch.alert.script.directory not in ambari.properties
    final String filename = "foo.py";
    notification.DispatchProperties.put(AlertScriptDispatcher.DISPATCH_PROPERTY_SCRIPT_FILENAME_KEY,filename);
    Assert.assertEquals(dispatcher.getScriptLocation(notification),"/var/lib/ambari-server/resources/scripts/foo.py");

    //3.both properties are set
    final String scriptDirectory = "/var/lib/ambari-server/resources/scripts/foo";
    m_configuration.setProperty(DISPATCH_PROPERTY_SCRIPT_DIRECTORY_KEY,scriptDirectory);
    Assert.assertEquals(dispatcher.getScriptLocation(notification),"/var/lib/ambari-server/resources/scripts/foo/foo.py");
  }


  /**
   * Tests that we will pickup the correct script when script filename is specified on the notification
   */
  @Test
  public void testCustomScriptConfigurationByScriptFilename() throws Exception {
    final String filename = "foo.py";
    final String scriptDirectory = "/var/lib/ambari-server/resources/scripts/foo";
    m_configuration.setProperty(DISPATCH_PROPERTY_SCRIPT_DIRECTORY_KEY,scriptDirectory);

    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    notification.DispatchProperties = new HashMap();
    notification.DispatchProperties.put(AlertScriptDispatcher.DISPATCH_PROPERTY_SCRIPT_FILENAME_KEY,filename);

    callback.onSuccess(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    AlertScriptDispatcher dispatcher = (AlertScriptDispatcher) m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    m_injector.injectMembers(dispatcher);

    ProcessBuilder powerMockProcessBuilder = m_injector.getInstance(ProcessBuilder.class);
    EasyMock.expect(dispatcher.getProcessBuilder(dispatcher.getScriptLocation(notification), notification)).andReturn(
            powerMockProcessBuilder).once();

    EasyMock.replay(callback, dispatcher);

    dispatcher.dispatch(notification);

    EasyMock.verify(callback, dispatcher);
    PowerMock.verifyAll();
  }


  /**
   * Tests that a process with an error code of 255 causes the failure callback
   * to be invoked.
   *
   * @throws Exception
   */
  @Test
  public void testFailedProcess() throws Exception {
    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    callback.onFailure(notification.CallbackIds);
    EasyMock.expectLastCall().once();

    AlertScriptDispatcher dispatcher = (AlertScriptDispatcher) m_dispatchFactory.getDispatcher(TargetType.ALERT_SCRIPT.name());
    m_injector.injectMembers(dispatcher);

    ProcessBuilder powerMockProcessBuilder = m_injector.getInstance(ProcessBuilder.class);
    EasyMock.expect(dispatcher.getProcessBuilder(SCRIPT_CONFIG_VALUE, notification)).andReturn(
        powerMockProcessBuilder).once();

    Process mockProcess = powerMockProcessBuilder.start();
    EasyMock.expect(mockProcess.exitValue()).andReturn(255).anyTimes();

    EasyMock.replay(callback, dispatcher, mockProcess);

    dispatcher.dispatch(notification);

    EasyMock.verify(callback, dispatcher);
    PowerMock.verifyAll();
  }

  /**
   * Tests that arguments given to the {@link ProcessBuilder} are properly
   * escaped.
   *
   * @throws Exception
   */
  @Test
  public void testArgumentEscaping() throws Exception {
    final String ALERT_DEFINITION_NAME = "mock_alert_with_quotes";
    final String ALERT_DEFINITION_LABEL = "Mock alert with Quotes";
    final String ALERT_LABEL = "Alert Label";
    final String ALERT_SERVICE_NAME = "FOO_SERVICE";
    final String ALERT_TEXT = "Did you know, \"Quotes are hard!!!\"";
    final String ALERT_TEXT_ESCAPED = "Did you know, \\\"Quotes are hard\\!\\!\\!\\\"";
    final String ALERT_HOST = "mock_host";
    final long ALERT_TIMESTAMP = 1111111l;

    DispatchCallback callback = EasyMock.createNiceMock(DispatchCallback.class);
    AlertNotification notification = new AlertNotification();
    notification.Callback = callback;
    notification.CallbackIds = Collections.singletonList(UUID.randomUUID().toString());

    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    definition.setDefinitionName(ALERT_DEFINITION_NAME);
    definition.setLabel(ALERT_DEFINITION_LABEL);

    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertDefinition(definition);
    history.setAlertLabel(ALERT_LABEL);
    history.setAlertText(ALERT_TEXT);
    history.setAlertState(AlertState.OK);
    history.setServiceName(ALERT_SERVICE_NAME);
    history.setHostName(ALERT_HOST);
    history.setAlertTimestamp(ALERT_TIMESTAMP);


    AlertInfo alertInfo = new AlertInfo(history);
    notification.setAlertInfo(alertInfo);

    AlertScriptDispatcher dispatcher = new AlertScriptDispatcher();
    m_injector.injectMembers(dispatcher);

    ProcessBuilder processBuilder = dispatcher.getProcessBuilder(SCRIPT_CONFIG_VALUE, notification);
    List<String> commands = processBuilder.command();
    Assert.assertEquals(3, commands.size());
    Assert.assertEquals("sh", commands.get(0));
    Assert.assertEquals("-c", commands.get(1));

    StringBuilder buffer = new StringBuilder();
    buffer.append(SCRIPT_CONFIG_VALUE).append(" ");
    buffer.append(ALERT_DEFINITION_NAME).append(" ");
    buffer.append("\"").append(ALERT_DEFINITION_LABEL).append("\"").append(" ");
    buffer.append(ALERT_SERVICE_NAME).append(" ");
    buffer.append(AlertState.OK).append(" ");
    buffer.append("\"").append(ALERT_TEXT_ESCAPED).append("\"").append(" ");
    buffer.append(ALERT_TIMESTAMP).append(" ");
    buffer.append(ALERT_HOST);

    Assert.assertEquals(buffer.toString(), commands.get(2));

    //if hostname is null
    history.setHostName(null);
    alertInfo = new AlertInfo(history);
    notification.setAlertInfo(alertInfo);

    processBuilder = dispatcher.getProcessBuilder(SCRIPT_CONFIG_VALUE, notification);
    commands = processBuilder.command();
    buffer = new StringBuilder();
    buffer.append(SCRIPT_CONFIG_VALUE).append(" ");
    buffer.append(ALERT_DEFINITION_NAME).append(" ");
    buffer.append("\"").append(ALERT_DEFINITION_LABEL).append("\"").append(" ");
    buffer.append(ALERT_SERVICE_NAME).append(" ");
    buffer.append(AlertState.OK).append(" ");
    buffer.append("\"").append(ALERT_TEXT_ESCAPED).append("\"").append(" ");
    buffer.append(ALERT_TIMESTAMP).append(" ");
    buffer.append("");
    Assert.assertEquals(buffer.toString(), commands.get(2));

  }

  /**
   *
   */
  private class MockModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
      try {
        // populate configuration with the 1 config we need
        Configuration configuration = new Configuration();
        configuration.setProperty(AlertScriptDispatcher.SCRIPT_CONFIG_DEFAULT_KEY, SCRIPT_CONFIG_VALUE);

        // do some basic bindings
        DispatchFactory dispatchFactory = DispatchFactory.getInstance();
        bind(DispatchFactory.class).toInstance(dispatchFactory);
        bind(Configuration.class).toInstance(configuration);
        bind(OsFamily.class).toInstance(EasyMock.createNiceMock(OsFamily.class));

        // mock the dispatcher to return our doctored ProcessBuilder
        AlertScriptDispatcher dispatcher = EasyMock.createMockBuilder(AlertScriptDispatcher.class).addMockedMethods(
            "getProcessBuilder").createNiceMock();

        // make the executor synchronized for testing
        SynchronizedExecutor synchronizedExecutor = new SynchronizedExecutor();
        Field field = AlertScriptDispatcher.class.getDeclaredField("m_executor");
        field.setAccessible(true);
        field.set(dispatcher, synchronizedExecutor);

        // since we're bypassing normal bootstrapping, register the dispatcher
        // manually
        dispatchFactory.register(dispatcher.getType(), dispatcher);

        // bind the dispatcher to force member injection
        bind(AlertScriptDispatcher.class).toInstance(dispatcher);

        Process processMock = EasyMock.createNiceMock(Process.class);

        // use powermock since EasyMock can't mock final classes
        ProcessBuilder powerMockProcessBuilder = PowerMock.createNiceMock(ProcessBuilder.class);
        EasyMock.expect(powerMockProcessBuilder.start()).andReturn(processMock).atLeastOnce();
        PowerMock.replay(powerMockProcessBuilder);

        // bind the doctored ProcessBuilder so we can use it from anywhere
        bind(ProcessBuilder.class).toInstance(powerMockProcessBuilder);
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * The {@link SynchronizedExecutor} is used to execute {@link Runnable}s
   * serially.
   */
  private static final class SynchronizedExecutor implements Executor {

    /**
     * @param command
     */
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  }
}
