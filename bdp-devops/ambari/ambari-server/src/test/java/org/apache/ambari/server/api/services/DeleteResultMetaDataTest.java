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

package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import com.google.common.collect.Sets;

public class DeleteResultMetaDataTest {

  @Test
  public void testDeletedKeys() {
    String key1 = "key1";
    String key2 = "key2";
    DeleteResultMetadata metadata = new DeleteResultMetadata();
    metadata.addDeletedKey(key1);
    metadata.addDeletedKey(key2);
    assertTrue(CollectionUtils.isEqualCollection(Sets.newHashSet(key1, key2), metadata.getDeletedKeys()));
    assertTrue(metadata.getExcptions().isEmpty());
  }

  @Test
  public void testException() {
    String key1 = "key1";
    String key2 = "key2";
    String key3 = "key3";
    String key4 = "key4";
    String key5 = "key5";
    DeleteResultMetadata metadata = new DeleteResultMetadata();
    metadata.addException(key1, new AuthorizationException("Exception"));
    metadata.addException(key2, new SystemException("Exception"));
    metadata.addException(key3, new HostNotFoundException("Exception"));
    metadata.addException(key4, new UnsupportedPropertyException(Resource.Type.Action, Collections.emptySet()));
    metadata.addException(key5, new NullPointerException());

    assertTrue(metadata.getDeletedKeys().isEmpty());
    Map<String, ResultStatus> resultStatusMap =  metadata.getExcptions();
    assertEquals(resultStatusMap.get(key1).getStatus(), ResultStatus.STATUS.FORBIDDEN);
    assertEquals(resultStatusMap.get(key2).getStatus(), ResultStatus.STATUS.SERVER_ERROR);
    assertEquals(resultStatusMap.get(key3).getStatus(), ResultStatus.STATUS.NOT_FOUND);
    assertEquals(resultStatusMap.get(key4).getStatus(), ResultStatus.STATUS.BAD_REQUEST);
    assertEquals(resultStatusMap.get(key5).getStatus(), ResultStatus.STATUS.SERVER_ERROR);
  }
}
