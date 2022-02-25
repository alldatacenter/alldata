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

package org.apache.ambari.server.controller.internal;

import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.junit.Assert.assertTrue;

import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.junit.Test;

import com.google.common.collect.Sets;

public class DeleteStatusMetaDataTest {
  private String key1 = "key1";
  private String key2 = "key2";

  @Test
  public void testDeletedKeys() {
    DeleteStatusMetaData metaData = new DeleteStatusMetaData();
    metaData.addDeletedKey(key1);
    metaData.addDeletedKey(key2);
    assertTrue(isEqualCollection(Sets.newHashSet(key1, key2), metaData.getDeletedKeys()));
    assertTrue(metaData.getExceptionForKeys().isEmpty());
  }

  @Test
  public void testExceptions() {
    DeleteStatusMetaData metaData = new DeleteStatusMetaData();
    metaData.addException(key1, new SystemException("test"));
    metaData.addException(key2, new ObjectNotFoundException("test"));
    assertTrue(metaData.getExceptionForKeys().get(key1) instanceof SystemException);
    assertTrue(metaData.getExceptionForKeys().get(key2) instanceof ObjectNotFoundException);
  }
}
