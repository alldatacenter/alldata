/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.plugin.utils;

import io.debezium.embedded.EmbeddedEngine;
import org.apache.inlong.agent.pojo.DebeziumOffset;
import org.apache.inlong.agent.utils.DebeziumOffsetSerializer;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetStorageWriter;
import org.apache.kafka.connect.util.SafeObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of OffsetBackingStore that saves data locally to a file. To ensure this behaves
 * similarly to a real backing store, operations are executed asynchronously on a background thread.
 * The offset position can be specified
 */
public class InLongFileOffsetBackingStore extends MemoryOffsetBackingStore {

    public static final String OFFSET_STATE_VALUE = "offset.storage.inlong.state.value";
    public static final int FLUSH_TIMEOUT_SECONDS = 10;
    private static final Logger log = LoggerFactory.getLogger(FileOffsetBackingStore.class);
    private File file;

    public InLongFileOffsetBackingStore() {

    }

    @Override
    public void configure(WorkerConfig config) {
        super.configure(config);
        file = new File(config.getString(StandaloneConfig.OFFSET_STORAGE_FILE_FILENAME_CONFIG));
        // eagerly initialize the executor, because OffsetStorageWriter will use it later
        start();

        Map<String, ?> conf = config.originals();
        if (!conf.containsKey(OFFSET_STATE_VALUE)) {
            // a normal startup from clean state, not need to initialize the offset
            return;
        }

        String stateJson = (String) conf.get(OFFSET_STATE_VALUE);
        DebeziumOffsetSerializer serializer = new DebeziumOffsetSerializer();
        DebeziumOffset debeziumOffset;
        try {
            debeziumOffset = serializer.deserialize(stateJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Can't deserialize debezium offset state from JSON: " + stateJson, e);
            throw new RuntimeException(e);
        }

        final String engineName = (String) conf.get(EmbeddedEngine.ENGINE_NAME.name());
        Converter keyConverter = new JsonConverter();
        Converter valueConverter = new JsonConverter();
        keyConverter.configure(config.originals(), true);
        Map<String, Object> valueConfigs = new HashMap<>(conf);
        valueConfigs.put("schemas.enable", false);
        valueConverter.configure(valueConfigs, true);
        OffsetStorageWriter offsetWriter =
                new OffsetStorageWriter(
                        this,
                        // must use engineName as namespace to align with Debezium Engine
                        // implementation
                        engineName,
                        keyConverter,
                        valueConverter);

        offsetWriter.offset(debeziumOffset.sourcePartition, debeziumOffset.sourceOffset);

        // flush immediately
        if (!offsetWriter.beginFlush()) {
            // if nothing is needed to be flushed, there must be something wrong with the
            // initialization
            log.warn(
                    "Initialize InLongFileOffsetBackingStore from empty offset state, this shouldn't happen.");
            return;
        }

        // trigger flushing
        Future<Void> flushFuture =
                offsetWriter.doFlush(
                        (error, result) -> {
                            if (error != null) {
                                log.error("Failed to flush initial offset.", error);
                            } else {
                                log.debug("Successfully flush initial offset.");
                            }
                        });

        // wait until flushing finished
        try {
            flushFuture.get(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info(
                    "Flush offsets successfully, partition: {}, offsets: {}",
                    debeziumOffset.sourcePartition,
                    debeziumOffset.sourceOffset);
        } catch (InterruptedException e) {
            log.warn("Flush offsets interrupted, cancelling.", e);
            offsetWriter.cancelFlush();
        } catch (ExecutionException e) {
            log.error("Flush offsets threw an unexpected exception.", e);
            offsetWriter.cancelFlush();
        } catch (TimeoutException e) {
            log.error("Timed out waiting to flush offsets to storage.", e);
            offsetWriter.cancelFlush();
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        log.info("Starting FileOffsetBackingStore with file {}", file);
        load();
    }

    @Override
    public synchronized void stop() {
        super.stop();
        // Nothing to do since this doesn't maintain any outstanding connections/data
        log.info("Stopped FileOffsetBackingStore");
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try (SafeObjectInputStream is = new SafeObjectInputStream(Files.newInputStream(file.toPath()))) {
            Object obj = is.readObject();
            if (!(obj instanceof HashMap)) {
                throw new ConnectException("Expected HashMap but found " + obj.getClass());
            }
            Map<byte[], byte[]> raw = (Map<byte[], byte[]>) obj;
            data = new HashMap<>();
            for (Map.Entry<byte[], byte[]> mapEntry : raw.entrySet()) {
                ByteBuffer key = (mapEntry.getKey() != null) ? ByteBuffer.wrap(mapEntry.getKey()) : null;
                ByteBuffer value = (mapEntry.getValue() != null) ? ByteBuffer.wrap(mapEntry.getValue()) : null;
                data.put(key, value);
            }
        } catch (NoSuchFileException | EOFException e) {
            // NoSuchFileException: Ignore, may be new.
            // EOFException: Ignore, this means the file was missing or corrupt
        } catch (IOException | ClassNotFoundException e) {
            throw new ConnectException(e);
        }
    }

    @Override
    protected void save() {
        try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(file.toPath()))) {
            Map<byte[], byte[]> raw = new HashMap<>();
            for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
                byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
                byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
                raw.put(key, value);
            }
            os.writeObject(raw);
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }
}