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

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.history.AbstractDatabaseHistory;
import io.debezium.relational.history.DatabaseHistory;
import io.debezium.relational.history.DatabaseHistoryException;
import io.debezium.relational.history.DatabaseHistoryListener;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.HistoryRecordComparator;
import io.debezium.util.Collect;
import io.debezium.util.FunctionalReadWriteLock;
import org.apache.inlong.agent.plugin.message.SchemaRecord;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A {@link DatabaseHistory} implementation that stores the schema history in a local file.
 */
public class InLongDatabaseHistory extends AbstractDatabaseHistory {

    public static final String DATABASE_HISTORY_INSTANCE_NAME = "database.history.instance.name";
    public static final Field FILE_PATH = Field.create("database.history.file.filename")
            .withDescription("The path to the file that will be used to record the database history").required();
    private static final Charset UTF8;
    public static Collection<Field> ALL_FIELDS;

    static {
        ALL_FIELDS = Collect.arrayListOf(FILE_PATH, new Field[0]);
        UTF8 = StandardCharsets.UTF_8;
    }

    private final FunctionalReadWriteLock lock = FunctionalReadWriteLock.reentrant();
    private final DocumentWriter writer = DocumentWriter.defaultWriter();
    private final DocumentReader reader = DocumentReader.defaultReader();
    private final AtomicBoolean running = new AtomicBoolean();
    private ConcurrentLinkedQueue<SchemaRecord> schemaRecords;
    private String instanceName;
    private Path path;

    public InLongDatabaseHistory() {
    }

    public static boolean isCompatible(Collection<SchemaRecord> records) {
        Iterator var1 = records.iterator();
        if (var1.hasNext()) {
            SchemaRecord record = (SchemaRecord) var1.next();
            if (!record.isHistoryRecord()) {
                return false;
            }
        }

        return true;
    }

    private ConcurrentLinkedQueue<SchemaRecord> getRegisteredHistoryRecord(String instanceName) {
        Collection<SchemaRecord> historyRecords = DatabaseHistoryUtil.retrieveHistory(instanceName);
        return new ConcurrentLinkedQueue(historyRecords);
    }

    public void configure(Configuration config, HistoryRecordComparator comparator, DatabaseHistoryListener listener,
            boolean useCatalogBeforeSchema) {
        Collection var10001 = ALL_FIELDS;
        Logger var10002 = this.logger;
        Objects.requireNonNull(var10002);
        if (!config.validateAndRecord(var10001, var10002::error)) {
            throw new ConnectException("Error configuring an instance of " + this.getClass().getSimpleName()
                    + "; check the logs for details");
        } else {
            if (this.running.get()) {
                throw new IllegalStateException("Database history file already initialized to " + this.path);
            } else {
                super.configure(config, comparator, listener, useCatalogBeforeSchema);
                this.instanceName = config.getString("database.history.instance.name");
                this.schemaRecords = this.getRegisteredHistoryRecord(this.instanceName);
                DatabaseHistoryUtil.registerHistory(this.instanceName, this.schemaRecords);
                this.path = Paths.get(config.getString(FILE_PATH));
            }
        }
    }

    public void start() {
        super.start();
        this.lock.write(() -> {
            if (this.running.compareAndSet(false, true)) {
                Path path = this.path;
                if (path == null) {
                    throw new IllegalStateException("FileDatabaseHistory must be configured before it is started");
                }

                try {
                    if (!this.storageExists()) {
                        if (path.getParent() != null && !Files.exists(path.getParent(), new LinkOption[0])) {
                            Files.createDirectories(path.getParent());
                        }

                        try {
                            Files.createFile(path);
                        } catch (FileAlreadyExistsException var3) {
                            // do nothing
                        }
                    }
                } catch (IOException var4) {
                    throw new DatabaseHistoryException(
                            "Unable to create history file at " + path + ": " + var4.getMessage(), var4);
                }
            }

        });
    }

    public void stop() {
        this.running.set(false);
        super.stop();
        DatabaseHistoryUtil.removeHistory(this.instanceName);
    }

    protected void storeRecord(HistoryRecord record) throws DatabaseHistoryException {
        if (record != null) {
            this.lock.write(() -> {
                if (!this.running.get()) {
                    throw new IllegalStateException("The history has been stopped and will not accept more records");
                } else {
                    try {
                        String line = this.writer.write(record.document());

                        try {
                            BufferedWriter historyWriter = Files
                                    .newBufferedWriter(this.path, StandardOpenOption.APPEND);

                            label58: {
                                try {
                                    try {
                                        historyWriter.append(line);
                                        historyWriter.newLine();
                                        this.schemaRecords.add(new SchemaRecord(record));
                                        break label58;
                                    } catch (IOException var7) {
                                        this.logger.error("Failed to add record to history at {}: {}",
                                                new Object[]{this.path, record, var7});
                                    }
                                } catch (Throwable var8) {
                                    if (historyWriter != null) {
                                        try {
                                            historyWriter.close();
                                        } catch (Throwable var6) {
                                            var8.addSuppressed(var6);
                                        }
                                    }

                                    throw var8;
                                }

                                if (historyWriter != null) {
                                    historyWriter.close();
                                }

                                return;
                            }

                            if (historyWriter != null) {
                                historyWriter.close();
                            }
                        } catch (IOException var9) {
                            throw new DatabaseHistoryException(
                                    "Unable to create writer for history file " + this.path + ": " + var9.getMessage(),
                                    var9);
                        }
                    } catch (IOException var10) {
                        this.logger.error("Failed to convert record to string: {}", record, var10);
                    }

                }
            });
        }
    }

    protected synchronized void recoverRecords(Consumer<HistoryRecord> records) {
        this.lock.write(() -> {
            try {
                if (this.exists()) {
                    Iterator var2 = Files.readAllLines(this.path, UTF8).iterator();

                    while (var2.hasNext()) {
                        String line = (String) var2.next();
                        if (line != null && !line.isEmpty()) {
                            records.accept(new HistoryRecord(this.reader.read(line)));
                        }
                    }
                }
            } catch (IOException var4) {
                this.logger.error("Failed to add recover records from history at {}", this.path, var4);
            }

        });
        this.schemaRecords.stream().map(SchemaRecord::getHistoryRecord).forEach(records);
    }

    public boolean exists() {
        return !this.schemaRecords.isEmpty();
    }

    public boolean storageExists() {
        return Files.exists(path);
    }

    public String toString() {
        return "file " + (this.path != null ? this.path : "(unstarted)");
    }
}
