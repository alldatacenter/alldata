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

package org.apache.atlas.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.atlas.model.notification.AtlasNotificationMessage;
import org.apache.atlas.model.notification.MessageVersion;
import org.slf4j.Logger;

/**
 * Base notification message deserializer.
 */
public abstract class AbstractMessageDeserializer<T> extends AtlasNotificationMessageDeserializer<T> {

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a deserializer.
     *
     * @param expectedVersion         the expected message version
     * @param notificationLogger      logger for message version mismatch
     */
    public AbstractMessageDeserializer(TypeReference<T> messageType,
                                       TypeReference<AtlasNotificationMessage<T>> notificationMessageType,
                                       MessageVersion expectedVersion, Logger notificationLogger) {
        super(messageType, notificationMessageType, expectedVersion, notificationLogger);
    }


    // ----- helper methods --------------------------------------------------
}
