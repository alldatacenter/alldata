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
package io.datavines.server.catalog.metadata;

import io.datavines.common.utils.*;
import io.datavines.server.registry.Register;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchCommand;
import io.datavines.server.repository.entity.catalog.CatalogMetaDataFetchTask;
import io.datavines.server.repository.service.impl.JobExternalService;
import io.datavines.server.utils.SpringApplicationContext;
import lombok.extern.slf4j.Slf4j;

import static io.datavines.common.CommonConstants.SLEEP_TIME_MILLIS;
import static io.datavines.common.utils.CommonPropertyUtils.*;

@Slf4j
public class CatalogMetaDataFetchTaskScheduler extends Thread {

    private final String CATALOG_TASK_LOCK_KEY =
            CommonPropertyUtils.getString(CommonPropertyUtils.CATALOG_TASK_LOCK_KEY, CommonPropertyUtils.CATALOG_TASK_LOCK_KEY_DEFAULT);

    private static final int[] RETRY_BACKOFF = {1, 2, 3, 5, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};

    private final JobExternalService jobExternalService;

    private final CatalogMetaDataFetchTaskManager catalogMetaDataFetchTaskManager;

    private final Register register;

    public CatalogMetaDataFetchTaskScheduler(CatalogMetaDataFetchTaskManager catalogMetaDataFetchTaskManager, Register register){
        this.jobExternalService = SpringApplicationContext.getBean(JobExternalService.class);
        this.catalogMetaDataFetchTaskManager = catalogMetaDataFetchTaskManager;
        this.register = register;
    }

    @Override
    public void run() {
        log.info("catalog metadata fetch task scheduler started");

        int retryNum = 0;
        while (Stopper.isRunning()) {
            CatalogMetaDataFetchCommand command = null;
            try {
                boolean runCheckFlag = OSUtils.checkResource(
                        CommonPropertyUtils.getDouble(MAX_CPU_LOAD_AVG, MAX_CPU_LOAD_AVG_DEFAULT),
                        CommonPropertyUtils.getDouble(RESERVED_MEMORY, RESERVED_MEMORY_DEFAULT));

                if (!runCheckFlag) {
                    ThreadUtils.sleep(SLEEP_TIME_MILLIS);
                    continue;
                }

                register.blockUtilAcquireLock(CATALOG_TASK_LOCK_KEY);

                command = jobExternalService.getCatalogCommand();

                if (command != null) {

                    CatalogMetaDataFetchTask task = jobExternalService.executeCatalogCommand(command);
                    if (task != null) {
                        log.info("start submit catalog metadata fetch task : {} ", JSONUtils.toJsonString(task));
                        catalogMetaDataFetchTaskManager.putCatalogTask(task);
                        jobExternalService.deleteCatalogCommandById(command.getId());
                        log.info(String.format("submit success, catalog  metadata fetch task : %s", task.getParameter()) );
                    }

                    register.release(CATALOG_TASK_LOCK_KEY);
                    ThreadUtils.sleep(SLEEP_TIME_MILLIS);
                } else {
                    register.release(CATALOG_TASK_LOCK_KEY);
                    ThreadUtils.sleep(SLEEP_TIME_MILLIS * 2);
                }

                retryNum = 0;
            } catch (Exception e){
                retryNum++;

                log.error("schedule catalog metadata fetch task error ", e);
                ThreadUtils.sleep(SLEEP_TIME_MILLIS * RETRY_BACKOFF [retryNum % RETRY_BACKOFF.length]);
            } finally {
                register.release(CATALOG_TASK_LOCK_KEY);
            }
        }
    }
}
