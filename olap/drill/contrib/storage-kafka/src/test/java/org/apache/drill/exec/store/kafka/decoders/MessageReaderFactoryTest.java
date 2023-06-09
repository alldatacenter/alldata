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

import org.apache.drill.categories.KafkaStorageTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({KafkaStorageTest.class})
public class MessageReaderFactoryTest extends BaseTest {

  @Test
  public void testShouldThrowExceptionAsMessageReaderIsNull() {
    try {
      MessageReaderFactory.getMessageReader(null);
      Assert.fail("Message reader initialization succeeded even though it is null");
    } catch (UserException ue) {
      Assert.assertSame(ue.getErrorType(), ErrorType.VALIDATION);
      Assert.assertTrue(ue.getMessage().contains(
          "VALIDATION ERROR: Please configure message reader implementation using the property 'store.kafka.record.reader'"));
    }
  }

  @Test
  public void testShouldThrowExceptionAsMessageReaderHasNotImplementedMessageReaderIntf() {
    try {
      MessageReaderFactory.getMessageReader(MessageReaderFactoryTest.class.getName());
      Assert.fail("Message reader initialization succeeded even though class does not implement message reader interface");
    } catch (UserException ue) {
      Assert.assertSame(ue.getErrorType(), ErrorType.VALIDATION);
      Assert.assertTrue(ue.getMessage().contains(
          "VALIDATION ERROR: Message reader configured 'org.apache.drill.exec.store.kafka.decoders.MessageReaderFactoryTest' does not implement 'org.apache.drill.exec.store.kafka.decoders.MessageReader'"));
    }
  }

  @Test
  public void testShouldThrowExceptionAsNoClassFound() {
    try {
      MessageReaderFactory.getMessageReader("a.b.c.d");
      Assert.fail("Message reader initialization succeeded even though class does not exist");
    } catch (UserException ue) {
      Assert.assertSame(ue.getErrorType(), ErrorType.VALIDATION);
      Assert.assertTrue(ue.getMessage().contains("VALIDATION ERROR: Failed to initialize message reader : a.b.c.d"));
    }
  }

  @Test
  public void testShouldReturnJsonMessageReaderInstance() {
    MessageReader messageReader = MessageReaderFactory.getMessageReader(JsonMessageReader.class.getName());
    Assert.assertTrue(messageReader instanceof JsonMessageReader);
  }
}
