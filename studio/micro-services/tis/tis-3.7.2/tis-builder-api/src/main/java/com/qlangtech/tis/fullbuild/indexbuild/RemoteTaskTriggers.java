package com.qlangtech.tis.fullbuild.indexbuild;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-16 09:53
 **/
public class RemoteTaskTriggers {
    private final List<IRemoteTaskTrigger> dumpPhaseTasks = new ArrayList<>();
    private final List<IRemoteTaskTrigger> joinPhaseTasks = new ArrayList<>();
    private final ExecutorService executorService;

    public RemoteTaskTriggers(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void addDumpPhaseTask(IRemoteTaskTrigger dumpTsk) {
        this.dumpPhaseTasks.add(dumpTsk);
    }

    public void addJoinPhaseTask(IRemoteTaskTrigger joinTsk) {
        this.joinPhaseTasks.add(joinTsk);
    }

//    public void merge(RemoteTaskTriggers tskTriggers) {
//        this.dumpPhaseTasks.addAll(tskTriggers.dumpPhaseTasks);
//        this.joinPhaseTasks.addAll(tskTriggers.joinPhaseTasks);
//    }

    public List<IRemoteTaskTrigger> getDumpPhaseTasks() {
        return Collections.unmodifiableList(this.dumpPhaseTasks);
    }

    public List<IRemoteTaskTrigger> getJoinPhaseTasks() {
        return Collections.unmodifiableList(this.joinPhaseTasks);
    }

    public void allCancel() {
        Stream.concat(dumpPhaseTasks.stream(), joinPhaseTasks.stream()).forEach((trigger) -> {
            trigger.cancel();
        });
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                executorService.shutdownNow();
//                // Wait a while for tasks to respond to being cancelled
//                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
//                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
