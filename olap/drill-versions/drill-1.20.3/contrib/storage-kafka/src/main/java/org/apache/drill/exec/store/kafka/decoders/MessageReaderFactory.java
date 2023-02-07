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
package org.apache.drill.exec.store.kafka.decoders;

import org.apache.drill.common.exceptions.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageReaderFactory {

  private static final Logger logger = LoggerFactory.getLogger(MessageReaderFactory.class);

  /**
   * Initialize kafka message reader based on store.kafka.record.reader session
   * property
   *
   * @param messageReaderKlass
   *          value of store.kafka.record.reader session property
   * @return kafka message reader
   * @throws UserException
   *           in case of any message reader initialization
   */
  public static MessageReader getMessageReader(String messageReaderKlass) {
    if (messageReaderKlass == null) {
      throw UserException.validationError()
          .message("Please configure message reader implementation using the property 'store.kafka.record.reader'")
          .build(logger);
    }

    MessageReader messageReader = null;
    try {
      Class<?> klass = Class.forName(messageReaderKlass);
      if (MessageReader.class.isAssignableFrom(klass)) {
        messageReader = (MessageReader) klass.newInstance();
        logger.debug("Initialized Message Reader : {}", messageReader);
      }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw UserException.validationError().message("Failed to initialize message reader : %s", messageReaderKlass)
          .build(logger);
    }

    if (messageReader == null) {
      throw UserException.validationError().message("Message reader configured '%s' does not implement '%s'",
          messageReaderKlass, MessageReader.class.getName()).build(logger);
    }
    return messageReader;
  }
}
