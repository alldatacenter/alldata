/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

declare module flink.job.detail {

  export interface JobManagerAddress {
    host: string;
    port: number;
    uRL: string;
  }

  export interface ClusterCfg {
    clusterId: string;
    jobManagerAddress: JobManagerAddress;
    name: string;
  }

  export interface IncrJobStatus {
    savepointPaths: Array<FlinkSavepoint>;
  }

  export interface FlinkSavepoint {
    // {
    //   "createTimestamp":1650173119716,
    //   "path":"file:///opt/data/savepoint/savepoint_20220417132518248"
    // }
    createTimestamp: number;
    path: string;
  }


  export interface JobVertexMetrics {
    bytesRead: number;
    bytesReadComplete: boolean;
    bytesWritten: number;
    bytesWrittenComplete: boolean;
    recordsRead: number;
    recordsReadComplete: boolean;
    recordsWritten: number;
    recordsWrittenComplete: boolean;
  }

  export interface TasksPerState {
    RECONCILING: number;
    FAILED: number;
    RUNNING: number;
    CANCELING: number;
    CREATED: number;
    SCHEDULED: number;
    CANCELED: number;
    DEPLOYING: number;
    FINISHED: number;
    INITIALIZING: number;
  }

  export interface JobVertexInfo {
    duration: number;
    endTime: number;
    executionState: string;
    jobVertexID: string;
    jobVertexMetrics: JobVertexMetrics;
    maxParallelism: number;
    name: string;
    parallelism: number;
    startTime: any;
    tasksPerState: TasksPerState;
  }

  export interface JobVerticesPerState {
    RECONCILING: number;
    FAILED: number;
    RUNNING: number;
    CANCELING: number;
    CREATED: number;
    SCHEDULED: number;
    CANCELED: number;
    DEPLOYING: number;
    FINISHED: number;
    INITIALIZING: number;
  }

  export interface PerExecState {
    count: number;
    stateColor: string;
  }

  export interface Timestamps {
    RECONCILING: number;
    INITIALIZING: number;
    RUNNING: number;
    CANCELLING: number;
    SUSPENDED: number;
    RESTARTING: number;
    FINISHED: number;
    FAILED: number;
    CANCELED: number;
    FAILING: number;
    CREATED: number;
  }

  export interface FlinkJobDetail {
    clusterCfg: ClusterCfg;
    incrJobStatus: IncrJobStatus;
    cancelable: boolean;
    duration: number;
    endTime: number;
    jobId: string;
    jobStatus: string;
    statusColor: string;
    sources: JobVertexInfo[];
    jobVerticesPerState: PerExecState[];
    maxParallelism: number;
    name: string;
    now: number;
    startTime: number;
    stoppable: boolean;
    timestamps: Timestamps;
  }
}
