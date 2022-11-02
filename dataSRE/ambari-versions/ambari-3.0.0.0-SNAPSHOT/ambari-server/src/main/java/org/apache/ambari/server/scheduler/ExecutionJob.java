/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.scheduler;

import org.quartz.Job;

/**
 * Type of Quartz Job that can be executed by the @ExecutionScheduleManager
 */
public interface ExecutionJob extends Job {
  String NEXT_EXECUTION_JOB_NAME_KEY = "ExecutionJob.Name";
  String NEXT_EXECUTION_JOB_GROUP_KEY = "ExecutionJob.Group";
  String NEXT_EXECUTION_SEPARATION_SECONDS =
    "ExecutionJob.SeparationMinutes";
  String LINEAR_EXECUTION_JOB_GROUP =
    "LinearExecutionJobs";
  String LINEAR_EXECUTION_TRIGGER_GROUP =
    "LinearExecutionTriggers";
}
