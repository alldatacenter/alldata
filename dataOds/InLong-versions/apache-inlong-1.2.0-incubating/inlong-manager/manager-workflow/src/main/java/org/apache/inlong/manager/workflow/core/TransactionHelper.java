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

package org.apache.inlong.manager.workflow.core;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.exceptions.WorkflowNoRollbackException;
import org.apache.inlong.manager.common.exceptions.WorkflowRollbackOnceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.Assert;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * Transaction Helper, now deprecated because we use @Transactional instead
 */
@Slf4j
@Service
@Deprecated
public class TransactionHelper {

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Execute in transaction
     *
     * @param action Execution logic
     * @param propagationBehavior Dissemination mechanism
     * @return result
     */
    public <T> T execute(TransactionCallback<T> action, int propagationBehavior) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");

        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(propagationBehavior);

        TransactionStatus status = this.transactionManager.getTransaction(transactionDefinition);

        T result;
        try {
            result = action.doInTransaction(status);
        } catch (WorkflowRollbackOnceException e) {
            this.rollbackOnException(status, e);
            throw new WorkflowNoRollbackException(e.getMessage());
        } catch (WorkflowNoRollbackException e) {
            this.transactionManager.commit(status);
            throw e;
        } catch (Error | RuntimeException e) {
            this.rollbackOnException(status, e);
            throw e;
        } catch (Throwable e) {
            this.rollbackOnException(status, e);
            throw new UndeclaredThrowableException(e, "TransactionCallback threw undeclared checked exception");
        }

        this.transactionManager.commit(status);
        return result;
    }

    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
        Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");
        log.debug("Initiating transaction rollback on application exception", ex);

        try {
            this.transactionManager.rollback(status);
        } catch (TransactionSystemException e) {
            log.error("Application exception overridden by rollback exception", ex);
            e.initApplicationException(ex);
            throw e;
        } catch (Error | RuntimeException e) {
            log.error("Application exception overridden by rollback exception", ex);
            throw e;
        }
    }

}
