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

package org.apache.celeborn.common.network.server;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.meta.FileManagedBuffers;
import org.apache.celeborn.common.network.buffer.ManagedBuffer;

/**
 * StreamManager which allows registration of an Iterator&lt;ManagedBuffer&gt;, which are
 * individually fetched as chunks by the client. Each registered buffer is one chunk.
 */
public class ChunkStreamManager {
  private static final Logger logger = LoggerFactory.getLogger(ChunkStreamManager.class);

  private final AtomicLong nextStreamId;
  // StreamId -> StreamState
  protected final ConcurrentHashMap<Long, StreamState> streams;
  // ShuffleKey -> StreamId
  protected final ConcurrentHashMap<String, Set<Long>> shuffleStreamIds;

  /** State of a single stream. */
  protected static class StreamState {
    final FileManagedBuffers buffers;
    final String shuffleKey;

    // Used to keep track of the index of the buffer that the user has retrieved, just to ensure
    // that the caller only requests each chunk one at a time, in order.
    int curChunk = 0;

    // Used to keep track of the number of chunks being transferred and not finished yet.
    volatile long chunksBeingTransferred = 0L;

    StreamState(String shuffleKey, FileManagedBuffers buffers) {
      this.buffers = Preconditions.checkNotNull(buffers);
      this.shuffleKey = shuffleKey;
    }
  }

  public ChunkStreamManager() {
    // For debugging purposes, start with a random stream id to help identifying different streams.
    // This does not need to be globally unique, only unique to this class.
    nextStreamId = new AtomicLong((long) new Random().nextInt(Integer.MAX_VALUE) * 1000);
    streams = new ConcurrentHashMap<>();
    shuffleStreamIds = new ConcurrentHashMap<>();
  }

  public ManagedBuffer getChunk(long streamId, int chunkIndex, int offset, int len) {
    StreamState state = streams.get(streamId);
    if (state == null) {
      throw new IllegalStateException(
          String.format(
              "Stream %s for chunk %s is not registered(Maybe removed).", streamId, chunkIndex));
    } else if (chunkIndex >= state.buffers.numChunks()) {
      throw new IllegalStateException(
          String.format("Requested chunk index beyond end %s", chunkIndex));
    }

    FileManagedBuffers buffers = state.buffers;
    if (buffers.hasAlreadyRead(chunkIndex)) {
      throw new IllegalStateException(
          String.format("Chunk %s for stream %s has already been read.", chunkIndex, streamId));
    }
    ManagedBuffer nextChunk = buffers.chunk(chunkIndex, offset, len);

    if (state.buffers.isFullyRead()) {
      // Normally, when all chunks are returned to the client, the stream should be removed here.
      // But if there is a switch on the client side, it will not go here at this time, so we need
      // to remove the stream when the shuffle is expired, and release the unused buffer.
      logger.trace("Removing stream id {}", streamId);
      streams.remove(streamId);
      Set<Long> streamIds = shuffleStreamIds.get(state.shuffleKey);
      if (streamIds != null) {
        streamIds.remove(streamId);
      }
    }

    return nextChunk;
  }

  public static String genStreamChunkId(long streamId, int chunkId) {
    return String.format("%d_%d", streamId, chunkId);
  }

  // Parse streamChunkId to be stream id and chunk id. This is used when fetch remote chunk as a
  // stream.
  public static Pair<Long, Integer> parseStreamChunkId(String streamChunkId) {
    String[] array = streamChunkId.split("_");
    assert array.length == 2 : "Stream id and chunk index should be specified.";
    long streamId = Long.parseLong(array[0]);
    int chunkIndex = Integer.parseInt(array[1]);
    return ImmutablePair.of(streamId, chunkIndex);
  }

  public void chunkBeingSent(long streamId) {
    StreamState streamState = streams.get(streamId);
    if (streamState != null) {
      streamState.chunksBeingTransferred++;
    }
  }

  public void chunkSent(long streamId) {
    StreamState streamState = streams.get(streamId);
    if (streamState != null) {
      streamState.chunksBeingTransferred--;
    }
  }

  public long chunksBeingTransferred() {
    long sum = 0L;
    for (StreamState streamState : streams.values()) {
      sum += streamState.chunksBeingTransferred;
    }
    return sum;
  }

  /**
   * Registers a stream of ManagedBuffers which are served as individual chunks one at a time to
   * callers. Each ManagedBuffer will be release()'d after it is transferred on the wire. If a
   * client connection is closed before the iterator is fully drained, then the remaining buffers
   * will all be release()'d.
   *
   * <p>If an app ID is provided, only callers who've authenticated with the given app ID will be
   * allowed to fetch from this stream.
   *
   * <p>This stream could be reused again when other channel of the client is reconnected. If a
   * stream is not properly closed, it will eventually be cleaned up by `cleanupExpiredShuffleKey`.
   */
  public long registerStream(String shuffleKey, FileManagedBuffers buffers) {
    long myStreamId = nextStreamId.getAndIncrement();
    streams.put(myStreamId, new StreamState(shuffleKey, buffers));
    shuffleStreamIds.compute(
        shuffleKey,
        (key, value) -> {
          if (value == null) {
            value = ConcurrentHashMap.newKeySet();
          }
          value.add(myStreamId);
          return value;
        });

    return myStreamId;
  }

  public void cleanupExpiredShuffleKey(Set<String> expiredShuffleKeys) {
    for (String expiredShuffleKey : expiredShuffleKeys) {
      Set<Long> expiredStreamIds = shuffleStreamIds.remove(expiredShuffleKey);

      // normally expiredStreamIds set will be empty as streamId will be removed when be fully read
      if (expiredStreamIds != null && !expiredStreamIds.isEmpty()) {
        streams.keySet().removeAll(expiredStreamIds);
      }
    }
  }

  @VisibleForTesting
  public int numStreamStates() {
    return streams.size();
  }

  @VisibleForTesting
  public long numShuffleSteams() {
    return shuffleStreamIds.values().stream().flatMap(Set::stream).count();
  }
}
