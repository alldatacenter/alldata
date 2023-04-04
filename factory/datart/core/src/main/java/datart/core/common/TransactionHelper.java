/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.common;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class TransactionHelper {

    public static TransactionStatus getTransaction(int propagationBehavior, int isolationLevel) {
        PlatformTransactionManager transactionManager = Application.getBean(PlatformTransactionManager.class);
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setIsolationLevel(isolationLevel);
        def.setPropagationBehavior(propagationBehavior);
        return transactionManager.getTransaction(def);
    }

    public static void commit(TransactionStatus transactionStatus) {
        Application.getBean(PlatformTransactionManager.class).commit(transactionStatus);
    }

    public static void rollback(TransactionStatus transactionStatus) {
        Application.getBean(PlatformTransactionManager.class).rollback(transactionStatus);
    }

}
