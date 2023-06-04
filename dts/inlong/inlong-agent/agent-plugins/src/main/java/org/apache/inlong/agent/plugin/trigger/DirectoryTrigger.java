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

package org.apache.inlong.agent.plugin.trigger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.FileTriggerType;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.plugin.Trigger;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_BLACKLIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_DIR_FILTER_PATTERNS;

/**
 * Watch directory, if new valid files are created, create jobs correspondingly.
 */
public class DirectoryTrigger implements Trigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryTrigger.class);
    private static volatile WatchService watchService;
    private static WatchKeyProviderThread resourceProviderThread = new WatchKeyProviderThread();
    private static ConcurrentHashMap<WatchKey, Set<DirectoryTrigger>> allTriggerWatches =
            new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<Map<String, String>> queue = new LinkedBlockingQueue<>();
    private Set<PathPattern> pathPatterns = new HashSet<>();
    private TriggerProfile profile;
    private int interval;

    private static void initWatchService() {
        try {
            if (watchService == null) {
                synchronized (DirectoryTrigger.class) {
                    if (watchService == null) {
                        watchService = FileSystems.getDefault().newWatchService();
                        LOGGER.info("init watch service {}", watchService);
                        new Thread(resourceProviderThread).start();
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("error while init watch service", ex);
        }
    }

    public TriggerProfile getProfile() {
        return profile;
    }

    @Override
    public Map<String, String> fetchJobProfile() {
        return queue.poll();
    }

    @Override
    public TriggerProfile getTriggerProfile() {
        return profile;
    }

    /**
     * register pathPattern into watchers, with offset
     */
    public Set<String> register(Set<String> whiteList, String offset, Set<String> blackList) throws IOException {
        this.pathPatterns = PathPattern.buildPathPattern(whiteList, offset, blackList);
        LOGGER.info("Watch root path is {}", pathPatterns);

        resourceProviderThread.initTrigger(this);
        return pathPatterns.stream().map(PathPattern::getRootDir).collect(Collectors.toSet());
    }

    @Override
    public void init(TriggerProfile profile) throws IOException {
        initWatchService();
        interval = profile.getInt(
                AgentConstants.TRIGGER_CHECK_INTERVAL, AgentConstants.DEFAULT_TRIGGER_CHECK_INTERVAL);
        this.profile = profile;
        if (this.profile.hasKey(JOB_DIR_FILTER_PATTERNS)) {
            Set<String> pathPatterns = Stream.of(
                    this.profile.get(JOB_DIR_FILTER_PATTERNS).split(",")).collect(Collectors.toSet());
            Set<String> blackList = Stream.of(
                    this.profile.get(JOB_DIR_FILTER_BLACKLIST, "").split(","))
                    .filter(black -> !StringUtils.isBlank(black))
                    .collect(Collectors.toSet());
            String timeOffset = this.profile.get(JobConstants.JOB_FILE_TIME_OFFSET, "");
            register(pathPatterns, timeOffset, blackList);
        }
    }

    @Override
    public void run() {
    }

    @Override
    public void destroy() {
        try {
            resourceProviderThread.destroyTrigger(this);
        } catch (Exception ex) {
            LOGGER.error("exception while stopping threads", ex);
        }
    }

    @VisibleForTesting
    public Collection<Map<String, String>> getFetchedJob() {
        return queue;
    }

    @VisibleForTesting
    public Map<WatchKey, Set<DirectoryTrigger>> getWatchers() {
        return allTriggerWatches;
    }

    public static class WatchKeyProviderThread implements Runnable {

        private final Object lock = new Object();

        @Override
        public void run() {
            Thread.currentThread().setName("Directory watch checker");
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(AgentConstants.DEFAULT_TRIGGER_CHECK_INTERVAL);
                    synchronized (lock) {
                        Map<WatchKey, Set<DirectoryTrigger>> addWatches = new HashMap<>();
                        Set<WatchKey> delWatches = new HashSet<>();
                        allTriggerWatches.forEach(
                                (watchKey, triggers) -> checkNewDir(triggers, watchKey, addWatches, delWatches));

                        addWatches.forEach(((watchKey, triggers) -> allTriggerWatches.compute(watchKey,
                                (existWatchKey, existsTriggers) -> {
                                    if (existsTriggers == null) {
                                        return triggers;
                                    }
                                    existsTriggers.addAll(triggers);
                                    return existsTriggers;
                                })));
                        delWatches.forEach(watchKey -> {
                            allTriggerWatches.remove(watchKey);
                            watchKey.cancel();
                        });
                    }
                } catch (Throwable ex) {
                    LOGGER.error("error caught", ex);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
                }
            }
        }

        public void initTrigger(DirectoryTrigger trigger) {
            synchronized (lock) {
                LOGGER.info("Init trigger_{} add watchKey.", trigger.getTriggerProfile().getTriggerId());
                checkInitDir(trigger);
                LOGGER.info("Init trigger_{} add watchKey end.", trigger.getTriggerProfile().getTriggerId());
            }
        }

        public void destroyTrigger(DirectoryTrigger trigger) {
            synchronized (lock) {
                LOGGER.info("Destroy trigger_{}.", trigger.getTriggerProfile().getTriggerId());
                for (Entry<WatchKey, Set<DirectoryTrigger>> entry : allTriggerWatches.entrySet()) {
                    entry.getValue().remove(trigger);
                }
                Set<WatchKey> watchKeys = new HashSet<>(allTriggerWatches.keySet());
                watchKeys.forEach(watchKey -> {
                    if (allTriggerWatches.containsKey(watchKey) && allTriggerWatches.get(watchKey).size() == 0) {
                        watchKey.cancel();
                        allTriggerWatches.remove(watchKey);
                    }
                });
                LOGGER.info("Destroy trigger_{} end.", trigger.getTriggerProfile().getTriggerId());
            }
        }

        private void checkInitDir(DirectoryTrigger trigger) {
            boolean registerSubFile = FileTriggerType.FULL.equals(
                    trigger.getTriggerProfile().get(JobConstants.JOB_FILE_TRIGGER_TYPE, FileTriggerType.FULL));
            trigger.pathPatterns.forEach(pathPattern -> {
                Set<WatchKey> tmpWatchers = new HashSet<>();
                registerAllSubDir(trigger, Paths.get(pathPattern.getRootDir()), tmpWatchers, registerSubFile);
                tmpWatchers.forEach(tmpWatch -> allTriggerWatches.compute(tmpWatch, (k, v) -> {
                    if (v == null) {
                        return Sets.newHashSet(trigger);
                    }
                    v.add(trigger);
                    return v;
                }));
            });
        }

        private void checkNewDir(
                Set<DirectoryTrigger> triggers,
                WatchKey watchKey,
                Map<WatchKey, Set<DirectoryTrigger>> addWatches,
                Set<WatchKey> delWatches) {
            Path parentPath = (Path) watchKey.watchable();
            if (!Files.exists(parentPath)) {
                LOGGER.warn("{} not exist, add watcher to pending delete list", parentPath);
                delWatches.add(watchKey);
                return;
            }

            Path appliedPath = parentPath;
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                // if watch event is too much, then event would be overflow.
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    // only watch create event, so else is create-event.
                    // entity.updateDateFormatRegex();
                    Path createdPath = (Path) event.context();
                    if (createdPath != null) {
                        appliedPath = parentPath.resolve(createdPath);
                    }
                } else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    // https://stackoverflow.com/questions/39076626/how-to-handle-the-java-watchservice-overflow-event
                    LOGGER.info("overflow got {}", parentPath);
                }

                final Path finalAppliedPath = appliedPath;
                triggers.forEach(trigger -> {
                    Set<WatchKey> tmpWatchers = new HashSet<>();
                    registerAllSubDir(trigger, finalAppliedPath, tmpWatchers, true);
                    tmpWatchers.forEach(tmpWatch -> addWatches.compute(tmpWatch, (k, v) -> {
                        if (v == null) {
                            return Sets.newHashSet(trigger);
                        }
                        v.add(trigger);
                        return v;
                    }));
                });
            }
        }

        private void registerAllSubDir(
                DirectoryTrigger trigger,
                Path path,
                Set<WatchKey> tobeAddedWatchers,
                boolean registerSubFile) {
            // check regex
            LOGGER.info("Check whether path {} is suitable for trigger_{}",
                    path, trigger.getTriggerProfile().getTriggerId());
            try {
                boolean isSuitable = trigger.pathPatterns.stream()
                        .filter(pathPattern -> pathPattern.suitable(path.toString())).findAny().isPresent();
                if (isSuitable) {
                    LOGGER.info("path {} is suitable for trigger_{}.",
                            path, trigger.getTriggerProfile().getTriggerId());
                    if (path.toFile().isDirectory()) {
                        WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                        tobeAddedWatchers.add(watchKey);
                        Files.list(path).forEach(subPath -> registerAllSubDir(trigger, subPath.toAbsolutePath(),
                                tobeAddedWatchers, registerSubFile));
                    } else if (registerSubFile) {
                        Map<String, String> taskProfile = new HashMap<>();
                        String md5 = AgentUtils.getFileMd5(path.toFile());
                        taskProfile.put(path.toFile().getAbsolutePath() + ".md5", md5);
                        taskProfile.put(JobConstants.JOB_TRIGGER, null); // del trigger id
                        taskProfile.put(JobConstants.JOB_DIR_FILTER_PATTERNS, path.toFile().getAbsolutePath());
                        LOGGER.info("trigger_{} generate job profile to read file {}",
                                trigger.getTriggerProfile().getTriggerId(), path);
                        trigger.queue.offer(taskProfile);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error happend", e);
            }
        }
    }
}
