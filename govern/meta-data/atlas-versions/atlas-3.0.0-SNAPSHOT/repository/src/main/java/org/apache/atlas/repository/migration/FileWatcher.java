/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.migration;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileWatcher {
    private static final Logger LOG = LoggerFactory.getLogger(FileWatcher.class);

    private final static int MAX_TIMES_PAUSE = 10;
    private final static int PAUSE_INTERVAL = 5000; // 5 secs

    private int checkIncrement;
    private final File fileToWatch;

    public FileWatcher(String filePath) {
        this.checkIncrement = 1;
        this.fileToWatch = new File(filePath);
    }

    public void start() throws IOException {
        if (existsAndReadyCheck()) {
            return;
        }

        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path pathToWatch = FileSystems.getDefault().getPath(fileToWatch.getParent());
        register(watcher, pathToWatch);

        try {
            LOG.info(String.format("Migration File Watcher: Watching: %s", fileToWatch.toString()));
            startWatching(watcher);
        } catch (InterruptedException ex) {
            LOG.error("Migration File Watcher: Interrupted!");
        } finally {
            watcher.close();
        }
    }

    private void startWatching(WatchService watcher) throws InterruptedException {
        while (true) {
            WatchKey watchKey = watcher.take();
            if (watchKey == null) {
                continue;
            }

            for (WatchEvent event : watchKey.pollEvents()) {
                if (checkIfFileAvailableAndReady(event)) {
                    return;
                }
            }

            watchKey.reset();
        }
    }

    private void register(WatchService watcher, Path path) throws IOException {
        try {
            path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LOG.error("Migration File Watcher: Error while registering event {}!", path);
            throw e;
        }
    }

    private boolean checkIfFileAvailableAndReady(WatchEvent event) {
        WatchEvent<Path> watchEvent = event;
        Path path = watchEvent.context();

        if (!path.toString().equals(fileToWatch.getName())) {
            return false;
        }

        return existsAndReadyCheck();
    }

    private boolean existsAndReadyCheck() {
        boolean ret = fileToWatch.exists() && fileToWatch.canRead();
        if (ret) {
            try {
                return isReadyForUse(fileToWatch);
            } catch (InterruptedException e) {
                LOG.error("Migration File Watcher: Interrupted {}!", fileToWatch);
                return false;
            }
        } else {
            LOG.info(String.format("Migration File Watcher: File does not exist!: %s", fileToWatch.getAbsolutePath()));
        }

        return ret;
    }

    private boolean isReadyForUse(File file) throws InterruptedException {
        Long fileSizeBefore = file.length();
        Thread.sleep(getCheckInterval());
        Long fileSizeAfter = file.length();
        boolean ret = fileSizeBefore.equals(fileSizeAfter);

        if (ret) {
            LOG.info(String.format("Migration File Watcher: %s: File is ready for use!", file.getAbsolutePath()));
        } else {
            incrementCheckCounter();
            LOG.info(
                    String.format("Migration File Watcher: File is being written: Pause: %,d secs: New size: %,d."
                            , getCheckInterval() / 1000
                            , fileSizeAfter));
        }

        return ret;
    }

    private int getCheckInterval() {
        return (PAUSE_INTERVAL * (checkIncrement));
    }

    private int incrementCheckCounter() {
        if (checkIncrement > MAX_TIMES_PAUSE) {
            checkIncrement = 1;
        }

        return (PAUSE_INTERVAL * (checkIncrement++));
    }
}
