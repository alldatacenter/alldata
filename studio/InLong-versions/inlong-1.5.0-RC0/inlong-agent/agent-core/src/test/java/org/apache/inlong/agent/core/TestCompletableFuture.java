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

package org.apache.inlong.agent.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCompletableFuture {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCompletableFuture.class);

    @Test
    public void testFuture() throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
            LOGGER.info("start run async");
        }, service);
        future.join();
        service.shutdown();
    }

    @Test
    public void testFutureException() throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        List<CompletableFuture<?>> result = new ArrayList<>();
        result.add(CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("test future1");
                TimeUnit.SECONDS.sleep(1);
                throw new RuntimeException("test exception1");
            } catch (InterruptedException ex) {
                LOGGER.error("ignore exception {}", ex.getMessage());
            }
        }, service));
        result.add(CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("test future2");
                TimeUnit.MILLISECONDS.sleep(200);
                throw new RuntimeException("test exception2");
            } catch (InterruptedException ex) {
                LOGGER.error("ignore exception {}", ex.getMessage());
            }
        }, service));
        try {
            CompletableFuture.allOf(result.toArray(new CompletableFuture[0])).join();
            Assert.fail("future should fail");
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains("test exception"));
        }

        service.shutdown();
    }

    @Test
    public void testFutureComplete() throws Exception {
        ExecutorService service = Executors.newCachedThreadPool();
        CompletableFuture<?> reader = CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                throw new RuntimeException("test exception1");
            } catch (InterruptedException ex) {
                LOGGER.error("ignore exception {}", ex.getMessage());
            }
        }, service);

        CompletableFuture<?> writer = CompletableFuture.runAsync(() -> {
            try {
                int count = 0;
                while (count < 10) {
                    LOGGER.info("test writer");
                    count += 1;
                    TimeUnit.MILLISECONDS.sleep(200);
                }

            } catch (InterruptedException ex) {
                LOGGER.error("ignore exception {}", ex.getMessage());
            }
        }, service);

        CompletableFuture.anyOf(reader, writer).exceptionally(ex -> {
            Assert.assertTrue(ex.getMessage().contains("test exception1"));
            Arrays.asList(reader, writer).forEach(future -> future.completeExceptionally(
                    new RuntimeException("all exception")));
            return null;
        }).join();
        Assert.assertTrue(reader.isCompletedExceptionally());
        Assert.assertTrue(writer.isCompletedExceptionally());
        service.shutdown();
    }
}
