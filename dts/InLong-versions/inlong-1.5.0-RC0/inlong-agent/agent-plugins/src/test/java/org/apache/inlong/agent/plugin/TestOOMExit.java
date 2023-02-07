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

package org.apache.inlong.agent.plugin;

import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_ENABLE_OOM_EXIT;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ThreadUtils.class)
@PowerMockIgnore({"javax.management.*"})
public class TestOOMExit {

    @BeforeClass
    public static void setup() throws Exception {
        PowerMockito.spy(ThreadUtils.class);
        PowerMockito.doNothing().when(ThreadUtils.class, "forceShutDown");
    }

    @Test
    public void testOOM() {
        MockJobManager jobManager = new MockJobManager();
        AgentConfiguration conf = AgentConfiguration.getAgentConf();
        conf.setBoolean(AGENT_ENABLE_OOM_EXIT, true);
        jobManager.start();
        jobManager.join();
    }

    static class MockJobManager extends AbstractDaemon {

        private static final Logger LOGGER = LoggerFactory.getLogger(MockJobManager.class);

        @Override
        public void start() {
            submitWorker(throwOOMThread());
        }

        @Override
        public void stop() throws Exception {

        }

        public Runnable throwOOMThread() {
            return () -> {
                int i = 0;
                while (i < 5) {
                    try {
                        LOGGER.info("throw OOM thread: " + i);
                        TimeUnit.SECONDS.sleep(1);
                        i++;
                        if (i == 3) {
                            LOGGER.info("throw OOM");
                            throw new OutOfMemoryError();
                        }
                    } catch (Throwable ex) {
                        ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
                    }
                }
            };
        }
    }

}
