/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.worker.storage;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.celeborn.common.meta.FileManagedBuffers;

public class ChunkStreamManagerSuiteJ {
  @Test
  public void testStreamRegisterAndCleanup() {
    ChunkStreamManager manager = new ChunkStreamManager();

    @SuppressWarnings("unchecked")
    FileManagedBuffers buffers = Mockito.mock(FileManagedBuffers.class);

    @SuppressWarnings("unchecked")
    FileManagedBuffers buffers2 = Mockito.mock(FileManagedBuffers.class);
    FileManagedBuffers buffers3 = Mockito.mock(FileManagedBuffers.class);
    FileManagedBuffers buffers4 = Mockito.mock(FileManagedBuffers.class);

    manager.registerStream("shuffleKey1", buffers, null);
    manager.registerStream("shuffleKey1", buffers2, null);
    manager.registerStream("shuffleKey2", buffers3, null);
    long stream3 = manager.registerStream("shuffleKey3", buffers4, null);
    Assert.assertEquals(4, manager.numStreamStates());
    Assert.assertEquals(manager.numStreamStates(), manager.numShuffleSteams());

    manager.cleanupExpiredShuffleKey(new HashSet<>(Arrays.asList("shuffleKey1", "shuffleKey2")));
    manager.cleanupExpiredShuffleKey(new HashSet<>(Arrays.asList("none_exit_shuffleKey")));

    Assert.assertEquals(1, manager.numStreamStates());
    Assert.assertEquals(manager.numStreamStates(), manager.numShuffleSteams());

    // stream removed when buffer fully read
    manager.streams.remove(stream3);
    manager.shuffleStreamIds.get("shuffleKey3").remove(stream3);
    Assert.assertEquals(0, manager.numStreamStates());
    Assert.assertEquals(manager.numStreamStates(), manager.numShuffleSteams());

    // cleanup shuffleKey3
    manager.cleanupExpiredShuffleKey(new HashSet<>(Arrays.asList("shuffleKey3")));
    Assert.assertEquals(manager.numStreamStates(), manager.numShuffleSteams());
  }
}
