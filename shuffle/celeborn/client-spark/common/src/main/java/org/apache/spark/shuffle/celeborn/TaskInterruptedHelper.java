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

package org.apache.spark.shuffle.celeborn;

import org.apache.spark.TaskContext;
import org.apache.spark.TaskKilledException;

public class TaskInterruptedHelper {

  /**
   * Apache Celeborn implement shuffle writer in java, Celeborn must catch InterruptedException.
   * According to the constraint of the ShuffleWriter interface, Celeborn cannot throw
   * InterruptedException. Since Spark executor side handle TaskKilledException and
   * InterruptedException in the same way and Spark throw TaskKilledException with the TaskContext's
   * kill reason, so here we throw the TaskKilledException.
   */
  public static void throwTaskKillException() {
    if (TaskContext.get().getKillReason().isDefined()) {
      throw new TaskKilledException(TaskContext.get().getKillReason().get());
    } else {
      throw new TaskKilledException();
    }
  }
}
