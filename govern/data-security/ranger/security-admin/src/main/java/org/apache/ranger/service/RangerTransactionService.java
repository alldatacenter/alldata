/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class RangerTransactionService {
    @Autowired
    @Qualifier(value = "transactionManager")
    PlatformTransactionManager txManager;

    private static final Logger LOG = LoggerFactory.getLogger(RangerTransactionService.class);

    private ScheduledExecutorService scheduler = null;

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(1);
    }

    @PreDestroy
    public void destroy() {
        try {
            LOG.info("attempt to shutdown RangerTransactionService");
            scheduler.shutdown();
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            LOG.error("RangerTransactionService tasks interrupted");
        }
        finally {
            if (!scheduler.isTerminated()) {
                LOG.info("cancel non-finished RangerTransactionService tasks");
            }
            scheduler.shutdownNow();
            LOG.info("RangerTransactionService shutdown finished");
        }
    }

    public void scheduleToExecuteInOwnTransaction(final Runnable task, final long delayInMillis) {
        try {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    if (task != null) {
                        try {
                            //Create new  transaction
                            TransactionTemplate txTemplate = new TransactionTemplate(txManager);
                            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                            txTemplate.execute(new TransactionCallback<Object>() {
                                public Object doInTransaction(TransactionStatus status) {
                                    task.run();
                                    return null;
                                }
                            });
                        } catch (Exception e) {
                            LOG.error("Failed to commit TransactionService transaction", e);
                            LOG.error("Ignoring...");
                        }
                    }
                }
            }, delayInMillis, MILLISECONDS);
        } catch (Exception e) {
            LOG.error("Failed to schedule TransactionService transaction:", e);
            LOG.error("Ignroing...");
        }
    }

}
