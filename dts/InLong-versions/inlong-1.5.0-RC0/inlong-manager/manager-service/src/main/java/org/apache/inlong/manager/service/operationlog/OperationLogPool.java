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

package org.apache.inlong.manager.service.operationlog;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.dao.entity.OperationLogEntity;
import org.apache.inlong.manager.dao.mapper.OperationLogEntityMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.apache.inlong.manager.common.consts.InlongConstants.ALIVE_TIME_MS;
import static org.apache.inlong.manager.common.consts.InlongConstants.QUEUE_SIZE;

/**
 * Operation log thread pool
 */
@Slf4j
@Component
public class OperationLogPool {

    private static final int TIMEOUT_SECOND = 30;
    private static final int THREAD_NUM = 3;
    private static final int BUFFER_SIZE = 500;

    private static final ArrayBlockingQueue<OperationLogEntity> OPERATION_POOL = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
            THREAD_NUM,
            THREAD_NUM,
            ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactoryBuilder().setNameFormat("inlong-operation-log-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private OperationLogEntityMapper operationLogMapper;

    public static void publish(OperationLogEntity operation) {
        if (!OPERATION_POOL.offer(operation)) {
            log.info("discard operation log: {}", operation);
        }
    }

    @PostConstruct
    public void init() {
        IntStream.range(0, THREAD_NUM).forEach(
                i -> EXECUTOR_SERVICE.submit(this::saveOperationLog));
    }

    private void saveOperationLog() {
        List<OperationLogEntity> buffer = new ArrayList<>(BUFFER_SIZE);
        while (true) {
            buffer.clear();
            try {
                int size = Queues.drain(OPERATION_POOL, buffer, BUFFER_SIZE, TIMEOUT_SECOND, TimeUnit.SECONDS);
                if (buffer.isEmpty()) {
                    continue;
                }
                long startTime = System.currentTimeMillis();
                operationLogMapper.insertBatch(buffer);
                log.info("receive {} logs and saved cost {} ms", size, System.currentTimeMillis() - startTime);
            } catch (InterruptedException e) {
                log.error("save operation log interrupted", e);
                break;
            } catch (Exception e) {
                log.error("save operation log error: ", e);
            }
        }
    }
}
