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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;

public class TestWatchDirTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWatchDirTrigger.class);
    private static DirectoryTrigger trigger;

    @ClassRule
    public static final TemporaryFolder WATCH_FOLDER = new TemporaryFolder();

    @Before
    public void setupEach() throws Exception {
        trigger = new DirectoryTrigger();
        TriggerProfile jobConf = TriggerProfile.parseJsonStr("");
        jobConf.set(JobConstants.JOB_ID, "1");
        trigger.init(jobConf);
        trigger.run();
    }

    @After
    public void teardownEach() {
        trigger.destroy();
        for (File file : WATCH_FOLDER.getRoot().listFiles()) {
            FileUtils.deleteQuietly(file);
        }
        trigger.getFetchedJob().clear();
    }

    public void registerPathPattern(Set<String> whiteList, Set<String> blackList, String offset) throws IOException {
        trigger.register(whiteList, offset, blackList);
    }

    @Test
    public void testWatchEntity() throws Exception {
        PathPattern a1 = new PathPattern("1",
                Collections.singleton(WATCH_FOLDER.getRoot().toString()), Sets.newHashSet());
        PathPattern a2 = new PathPattern("1",
                Collections.singleton(WATCH_FOLDER.getRoot().toString()), Sets.newHashSet());
        HashMap<PathPattern, Integer> map = new HashMap<>();
        map.put(a1, 10);
        Integer result = map.remove(a2);
        Assert.assertEquals(a1, a2);
        Assert.assertEquals(10, result.intValue());
    }

    @Test
    public void testBlackList() throws Exception {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return;
        }

        registerPathPattern(
                Sets.newHashSet(WATCH_FOLDER.getRoot().getAbsolutePath()
                        + File.separator + "**" + File.separator + "*.log"),
                Sets.newHashSet(WATCH_FOLDER.getRoot().getAbsolutePath() + File.separator + "tmp"),
                null);
        File file1 = WATCH_FOLDER.newFile("1.log");
        File tmp = WATCH_FOLDER.newFolder("tmp");
        File file2 = new File(tmp.getAbsolutePath() + File.separator + "2.log");
        file2.createNewFile();
        await().atMost(10, TimeUnit.SECONDS).until(() -> trigger.getFetchedJob().size() == 1);
        Collection<Map<String, String>> jobs = trigger.getFetchedJob();
        Set<String> jobPaths = jobs.stream()
                .map(job -> job.get(JobConstants.JOB_DIR_FILTER_PATTERNS))
                .collect(Collectors.toSet());
        Assert.assertTrue(jobPaths.contains(file1.getAbsolutePath()));
    }

    @Test
    public void testCreateBeforeWatch() throws Exception {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return;
        }

        File tmp = WATCH_FOLDER.newFolder("tmp");
        File file1 = new File(tmp.getAbsolutePath() + File.separator + "1.log");
        file1.createNewFile();
        registerPathPattern(
                Sets.newHashSet(
                        WATCH_FOLDER.getRoot().getAbsolutePath() + File.separator + "**" + File.separator + "*.log"),
                Collections.emptySet(), null);
        await().atMost(10, TimeUnit.SECONDS).until(() -> trigger.getFetchedJob().size() == 1);
    }

    @Test
    public void testWatchDeepMatch() throws Exception {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return;
        }

        registerPathPattern(
                Sets.newHashSet(
                        WATCH_FOLDER.getRoot().getAbsolutePath() + File.separator + "**" + File.separator + "*.log"),
                Collections.emptySet(), null);
        File tmp = WATCH_FOLDER.newFolder("tmp", "deep");
        File file4 = new File(tmp.getAbsolutePath() + File.separator + "1.log");
        file4.createNewFile();
        await().atMost(10, TimeUnit.SECONDS).until(() -> trigger.getFetchedJob().size() == 1);
    }

    @Test
    public void testMultiPattern() throws Exception {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return;
        }

        registerPathPattern(
                Sets.newHashSet(
                        WATCH_FOLDER.getRoot().getAbsolutePath() + File.separator + "tmp" + File.separator + "*.log",
                        WATCH_FOLDER.getRoot().getAbsolutePath() + File.separator + "**" + File.separator + "*.txt"),
                Collections.emptySet(), null);
        final File file1 = WATCH_FOLDER.newFile("1.txt");
        File file2 = WATCH_FOLDER.newFile("2.log");
        File file3 = WATCH_FOLDER.newFile("3.tar.gz");
        File tmp = WATCH_FOLDER.newFolder("tmp");
        File file4 = new File(tmp.getAbsolutePath() + File.separator + "4.txt");
        file4.createNewFile();
        File file5 = new File(tmp.getAbsolutePath() + File.separator + "5.log");
        file5.createNewFile();

        await().atMost(10, TimeUnit.SECONDS).until(() -> trigger.getFetchedJob().size() == 3);
        Collection<Map<String, String>> jobs = trigger.getFetchedJob();
        Set<String> jobPaths = jobs.stream()
                .map(job -> job.get(JobConstants.JOB_DIR_FILTER_PATTERNS))
                .collect(Collectors.toSet());
        Assert.assertTrue(jobPaths.contains(file1.getAbsolutePath()));
        Assert.assertTrue(jobPaths.contains(file4.getAbsolutePath()));
        Assert.assertTrue(jobPaths.contains(file5.getAbsolutePath()));
    }
}
