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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Transport;

import org.apache.ambari.server.notifications.DispatchCallback;
import org.apache.ambari.server.notifications.DispatchFactory;
import org.apache.ambari.server.notifications.Notification;
import org.apache.ambari.server.notifications.NotificationDispatcher;
import org.apache.ambari.server.notifications.Recipient;
import org.apache.ambari.server.notifications.TargetConfigurationResult;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.alert.TargetType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 *
 */
public class EmailDispatcherTest {

  private Injector m_injector;
  private DispatchFactory m_dispatchFactory;

  @Before
  public void before() throws Exception {
    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_dispatchFactory = m_injector.getInstance(DispatchFactory.class);
  }

  /**
   * Tests that an email without recipients causes a callback error.
   */
  @Test
  public void testNoRecipients() {
    Notification notification = new Notification();
    DispatchCallback callback = EasyMock.createMock(DispatchCallback.class);
    notification.Callback = callback;

    List<String> callbackIds = new ArrayList<>();
    callbackIds.add(UUID.randomUUID().toString());
    notification.CallbackIds = callbackIds;

    callback.onFailure(callbackIds);

    EasyMock.expectLastCall();
    EasyMock.replay(callback);

    NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(TargetType.EMAIL.name());
    dispatcher.dispatch(notification);

    EasyMock.verify(callback);
  }

  /**
   * Tests that an email without properties causes a callback error.
   */
  @Test
  public void testNoEmailPropeties() {
    Notification notification = new Notification();
    DispatchCallback callback = EasyMock.createMock(DispatchCallback.class);
    notification.Callback = callback;
    notification.Recipients = new ArrayList<>();

    Recipient recipient = new Recipient();
    recipient.Identifier = "foo";

    notification.Recipients.add(recipient);

    List<String> callbackIds = new ArrayList<>();
    callbackIds.add(UUID.randomUUID().toString());
    notification.CallbackIds = callbackIds;

    callback.onFailure(callbackIds);

    EasyMock.expectLastCall();
    EasyMock.replay(callback);

    NotificationDispatcher dispatcher = m_dispatchFactory.getDispatcher(TargetType.EMAIL.name());
    dispatcher.dispatch(notification);

    EasyMock.verify(callback);
  }

  @Test
  public void testValidateTargetConfig_invalidOnAuthenticationException() throws Exception {

    Map<String, Object> properties = new HashMap<>();
    Transport mockedTransport = EasyMock.createNiceMock(Transport.class);
    EmailDispatcher dispatcher = EasyMock.createMockBuilder(EmailDispatcher.class).
        addMockedMethods("getMailTransport").createNiceMock();

    EasyMock.expect(dispatcher.getMailTransport(properties)).andReturn(mockedTransport);
    mockedTransport.connect();
    EasyMock.expectLastCall().andThrow(new AuthenticationFailedException());

    EasyMock.replay(dispatcher, mockedTransport);

    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    Assert.assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateTargetConfig_invalidOnMessagingException() throws Exception {

    Map<String, Object> properties = new HashMap<>();
    Transport mockedTransport = EasyMock.createNiceMock(Transport.class);
    EmailDispatcher dispatcher = EasyMock.createMockBuilder(EmailDispatcher.class).
        addMockedMethods("getMailTransport").createNiceMock();

    EasyMock.expect(dispatcher.getMailTransport(properties)).andReturn(mockedTransport);
    mockedTransport.connect();
    EasyMock.expectLastCall().andThrow(new MessagingException());

    EasyMock.replay(dispatcher, mockedTransport);

    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    Assert.assertEquals(TargetConfigurationResult.Status.INVALID, configValidationResult.getStatus());
  }

  @Test
  public void testValidateTargetConfig_validIfNoErrors() throws Exception {

    Map<String, Object> properties = new HashMap<>();
    Transport mockedTransport = EasyMock.createNiceMock(Transport.class);
    EmailDispatcher dispatcher = EasyMock.createMockBuilder(EmailDispatcher.class).
        addMockedMethods("getMailTransport").createNiceMock();

    EasyMock.expect(dispatcher.getMailTransport(properties)).andReturn(mockedTransport);

    EasyMock.replay(dispatcher, mockedTransport);

    TargetConfigurationResult configValidationResult = dispatcher.validateTargetConfig(properties);
    Assert.assertEquals(TargetConfigurationResult.Status.VALID, configValidationResult.getStatus());
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     *
     */
    @Override
    public void configure(Binder binder) {
    }
  }
}
