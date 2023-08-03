/**
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

package org.apache.atlas.hook;

import org.apache.atlas.model.notification.MessageSource;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.notification.NotificationException;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.atlas.v1.model.notification.HookNotificationV1.EntityCreateRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;


public class AtlasHookTest {

    @Mock
    private NotificationInterface notificationInterface;

    @Mock
    private FailedMessagesLogger failedMessagesLogger;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test (timeOut = 10000)
    public void testNotifyEntitiesDoesNotHangOnException() throws Exception {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications = new ArrayList<>();
        doThrow(new NotificationException(new Exception())).when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 0, null, notificationInterface, false,
                failedMessagesLogger, source);
        // if we've reached here, the method finished OK.
    }

    @Test
    public void testNotifyEntitiesRetriesOnException() throws NotificationException {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications =
                new ArrayList<HookNotification>() {{
                    add(new EntityCreateRequest("user"));
                }
            };
        doThrow(new NotificationException(new Exception())).when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 2, null, notificationInterface, false,
                failedMessagesLogger, source);

        verify(notificationInterface, times(2)).
                send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
    }

    @Test
    public void testFailedMessageIsLoggedIfRequired() throws NotificationException {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications =
                new ArrayList<HookNotification>() {{
                    add(new EntityCreateRequest("user"));
                }
            };
        doThrow(new NotificationException(new Exception(), Arrays.asList("test message")))
                .when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 2, null, notificationInterface, true,
                failedMessagesLogger, source);

        verify(failedMessagesLogger, times(1)).log("test message");
    }

    @Test
    public void testFailedMessageIsNotLoggedIfNotRequired() throws NotificationException {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications = new ArrayList<>();
        doThrow(new NotificationException(new Exception(), Arrays.asList("test message")))
                .when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 2, null, notificationInterface, false,
                failedMessagesLogger, source);

        verifyZeroInteractions(failedMessagesLogger);
    }

    @Test
    public void testAllFailedMessagesAreLogged() throws NotificationException {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications =
                new ArrayList<HookNotification>() {{
                    add(new EntityCreateRequest("user"));
                }
            };
        doThrow(new NotificationException(new Exception(), Arrays.asList("test message1", "test message2")))
                .when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 2, null, notificationInterface, true,
                failedMessagesLogger, source);

        verify(failedMessagesLogger, times(1)).log("test message1");
        verify(failedMessagesLogger, times(1)).log("test message2");
    }

    @Test
    public void testFailedMessageIsNotLoggedIfNotANotificationException() throws Exception {
        MessageSource source                     = new MessageSource(this.getClass().getSimpleName());
        List<HookNotification> hookNotifications = new ArrayList<>();
        doThrow(new RuntimeException("test message")).when(notificationInterface)
                .send(NotificationInterface.NotificationType.HOOK, hookNotifications, source);
        AtlasHook.notifyEntitiesInternal(hookNotifications, 2, null, notificationInterface, true,
                failedMessagesLogger, source);

        verifyZeroInteractions(failedMessagesLogger);
    }
}
