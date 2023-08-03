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

package org.apache.inlong.sort.hive.filesystem;

import org.apache.inlong.sort.hive.util.CacheHolder;

import org.apache.flink.formats.hadoop.bulk.HadoopFileCommitter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import static org.apache.flink.util.Preconditions.checkArgument;

/**
 * The Hadoop file committer that directly rename the in-progress file to the target file. For
 * FileSystem like S3, renaming may lead to additional copies.
 */
public class HadoopRenameFileCommitter implements HadoopFileCommitter {

    private static final Logger LOG = LoggerFactory.getLogger(HadoopRenameFileCommitter.class);

    private final Configuration configuration;

    private Path targetFilePath;

    private Path tempFilePath;

    private boolean sinkMultipleEnable;

    public HadoopRenameFileCommitter(Configuration configuration,
            Path targetFilePath,
            boolean sinkMultipleEnable)
            throws IOException {
        this.configuration = configuration;
        this.targetFilePath = targetFilePath;
        this.tempFilePath = generateTempFilePath();
        this.sinkMultipleEnable = sinkMultipleEnable;
    }

    public HadoopRenameFileCommitter(Configuration configuration,
            Path targetFilePath,
            Path inProgressPath,
            boolean sinkMultipleEnable) {
        this.configuration = configuration;
        this.targetFilePath = targetFilePath;
        this.tempFilePath = inProgressPath;
        this.sinkMultipleEnable = sinkMultipleEnable;
    }

    @Override
    public Path getTargetFilePath() {
        return targetFilePath;
    }

    @Override
    public Path getTempFilePath() {
        return tempFilePath;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void preCommit() {
        // Do nothing.
    }

    @Override
    public void commit() throws IOException {
        if (sinkMultipleEnable) {
            commitMultiple(true);
        } else {
            rename(true);
        }
    }

    @Override
    public void commitAfterRecovery() throws IOException {
        if (sinkMultipleEnable) {
            commitMultiple(false);
        } else {
            rename(false);
        }
    }

    private void commitMultiple(boolean assertFileExists) throws IOException {
        LOG.info("file committer cache {}", CacheHolder.getFileCommitterHashMap());
        Iterator<Path> iterator = CacheHolder.getFileCommitterHashMap().keySet().iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (path.getName().equals(tempFilePath.getName())) {
                HadoopRenameFileCommitter committer = CacheHolder.getFileCommitterHashMap().get(path);
                committer.rename(assertFileExists);
                iterator.remove();
            }
        }
    }

    private void rename(boolean assertFileExists) throws IOException {
        FileSystem fileSystem = FileSystem.get(targetFilePath.toUri(), configuration);

        if (!fileSystem.exists(tempFilePath)) {
            if (assertFileExists) {
                throw new IOException(
                        String.format("In progress file(%s) not exists.", tempFilePath));
            } else {
                // By pass the re-commit if source file not exists.
                // TODO: in the future we may also need to check if the target file exists.
                return;
            }
        }

        try {
            // If file exists, it will be overwritten.
            fileSystem.rename(tempFilePath, targetFilePath);
        } catch (IOException e) {
            throw new IOException(
                    String.format(
                            "Could not commit file from %s to %s", tempFilePath, targetFilePath),
                    e);
        }
    }

    private Path generateTempFilePath() throws IOException {
        checkArgument(targetFilePath.isAbsolute(), "Target file must be absolute");

        FileSystem fileSystem = FileSystem.get(targetFilePath.toUri(), configuration);

        Path parent = targetFilePath.getParent();
        String name = targetFilePath.getName();

        while (true) {
            Path candidate =
                    new Path(parent, "." + name + ".inprogress." + UUID.randomUUID());
            if (!fileSystem.exists(candidate)) {
                return candidate;
            }
        }
    }
}