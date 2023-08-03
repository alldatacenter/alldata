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

package org.apache.ranger.common.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.OptimisticLockException;

@Component
public class RangerTransactionSynchronizationAdapter extends TransactionSynchronizationAdapter {

    @Autowired
    @Qualifier(value = "transactionManager")
    PlatformTransactionManager txManager;

    private static final Logger LOG = LoggerFactory.getLogger(RangerTransactionSynchronizationAdapter.class);

    private static final ThreadLocal<List<Runnable>> RUNNABLES = new ThreadLocal<List<Runnable>>();
    private static final ThreadLocal<List<Runnable>> RUNNABLES_AFTER_COMMIT = new ThreadLocal<List<Runnable>>();

    public void executeOnTransactionCompletion(Runnable runnable) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting new runnable {" + runnable + "} to run after completion");
        }

        /*
        From TransactionSynchronizationManager documentation:
        TransactionSynchronizationManager is a central helper that manages resources and transaction synchronizations per thread.
        Resource management code should only register synchronizations when this manager is active,
        which can be checked via isSynchronizationActive(); it should perform immediate resource cleanup else.
        If transaction synchronization isn't active, there is either no current transaction,
        or the transaction manager doesn't support transaction synchronization.

        Note: Synchronization is an Interface for transaction synchronization callbacks which is implemented by
        TransactionSynchronizationAdapter
        */

        if (!registerSynchronization()) {
            LOG.info("Transaction synchronization is NOT ACTIVE. Executing right now runnable {" + runnable + "}");
            runnable.run();
            return;
        }
        List<Runnable> threadRunnables = RUNNABLES.get();
        if (threadRunnables == null) {
            threadRunnables = new ArrayList<Runnable>();
            RUNNABLES.set(threadRunnables);
        }
        threadRunnables.add(runnable);
    }

    public void executeOnTransactionCommit(Runnable runnable) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Submitting new runnable {" + runnable + "} to run after transaction is committed");
        }

        /*
        From TransactionSynchronizationManager documentation:
        TransactionSynchronizationManager is a central helper that manages resources and transaction synchronizations per thread.
        Resource management code should only register synchronizations when this manager is active,
        which can be checked via isSynchronizationActive(); it should perform immediate resource cleanup else.
        If transaction synchronization isn't active, there is either no current transaction,
        or the transaction manager doesn't support transaction synchronization.

        Note: Synchronization is an Interface for transaction synchronization callbacks which is implemented by
        TransactionSynchronizationAdapter
        */

        if (!registerSynchronization()) {
            LOG.info("Transaction synchronization is NOT ACTIVE. Executing right now runnable {" + runnable + "}");
            runnable.run();
            return;
        }
        List<Runnable> threadRunnables = RUNNABLES_AFTER_COMMIT.get();
        if (threadRunnables == null) {
            threadRunnables = new ArrayList<Runnable>();
            RUNNABLES_AFTER_COMMIT.set(threadRunnables);
        }
        threadRunnables.add(runnable);
    }

    @Override
    public void afterCompletion(int status) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerTransactionSynchronizationAdapter.afterCompletion(status=" + (status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK") + ")");
        }

        final boolean isParentTransactionCommitted = status == STATUS_COMMITTED;

        List<Runnable> runnablesAfterCommit = RUNNABLES_AFTER_COMMIT.get();
        RUNNABLES_AFTER_COMMIT.remove();

        List<Runnable> runnables = RUNNABLES.get();
        RUNNABLES.remove();

        if (isParentTransactionCommitted) {
            // Run tasks scheduled to run after transaction is successfully committed
            runRunnables(runnablesAfterCommit, true);
        }

        // Run other tasks scheduled to run after transaction completes
        runRunnables(runnables, false);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerTransactionSynchronizationAdapter.afterCompletion(status=" + (status == STATUS_COMMITTED ? "COMMITTED" : "ROLLED_BACK") + ")");
        }
    }

    private boolean registerSynchronization() {
        final boolean ret = TransactionSynchronizationManager.isSynchronizationActive();
        if (ret) {
            List<Runnable> threadRunnablesOnCompletion = RUNNABLES.get();
            List<Runnable> threadRunnablesOnCommit = RUNNABLES_AFTER_COMMIT.get();
            if (threadRunnablesOnCompletion == null && threadRunnablesOnCommit == null) {
                TransactionSynchronizationManager.registerSynchronization(this);
            }
        }
        return ret;
    }

    private void runRunnables(final List<Runnable> runnables, final boolean isParentTransactionCommitted) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerTransactionSynchronizationAdapter.runRunnables()");
        }

        if (CollectionUtils.isNotEmpty(runnables)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing {" + runnables.size() + "} runnables");
            }
            for (Runnable runnable : runnables) {
                boolean isThisTransactionCommitted = false;
                do {
                    try {
                        //Create new  transaction
                        TransactionTemplate txTemplate = new TransactionTemplate(txManager);
                        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

                        Object result = txTemplate.execute(new TransactionCallback<Object>() {
                            public Object doInTransaction(TransactionStatus status) {
                                Object result = null;
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Executing runnable {" + runnable + "}");
                                }
                                try {
                                    runnable.run();
                                    result = runnable;
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("executed runnable " + runnable);
                                    }
                                } catch (OptimisticLockException optimisticLockException) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Failed to execute runnable " + runnable + "because of OpmimisticLockException");
                                    }
                                } catch (Throwable e) {
                                    LOG.error("Failed to execute runnable " + runnable, e);
                                }
                                return result;
                            }
                        });

                        isThisTransactionCommitted = result == runnable;

                    } catch (OptimisticLockException optimisticLockException) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Failed to commit TransactionService transaction for runnable:[" + runnable + "]");
                        }
                    } catch (TransactionSystemException tse) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Failed to commit TransactionService transaction, exception:[" + tse + "]");
                        }
                    } catch (Throwable e){
                        LOG.warn("Failed to commit TransactionService transaction, throwable:[" + e + "]");
                    }
                } while (isParentTransactionCommitted && !isThisTransactionCommitted);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No runnables to execute");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerTransactionSynchronizationAdapter.runRunnables()");
        }
    }
}
