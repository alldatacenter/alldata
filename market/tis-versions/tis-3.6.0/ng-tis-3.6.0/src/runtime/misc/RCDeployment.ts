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

import {BasicFormComponent} from "../../common/basic.form.component";
import {K8SRCSpec, K8SReplicsSpecComponent} from "../../common/k8s.replics.spec.component";
import FlinkJobDetail = flink.job.detail.FlinkJobDetail;
import {StepType} from "../../common/steps.component";
import {Descriptor} from "../../common/tis.plugin";

/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 *   This program is free software: you can use, redistribute, and/or modify
 *   it under the terms of the GNU Affero General Public License, version 3
 *   or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

export interface CpuLimit {
  unit: string;
  unitEmpty: boolean;
  val: number;
}

export interface CpuRequest {
  unit: string;
  unitEmpty: boolean;
  val: number;
}


export interface MemoryLimit {
  unit: string;
  unitEmpty: boolean;
  val: number;
}

export interface MemoryRequest {
  unit: string;
  unitEmpty: boolean;
  val: number;
}

export interface Status {
  availableReplicas: number;
  fullyLabeledReplicas: number;
  observedGeneration: number;
  readyReplicas: number;
  replicas: number;
}

export interface RCDeployment {
  cpuLimit: CpuLimit;
  cpuRequest: CpuRequest;
  creationTimestamp: number;
  dockerImage: string;
  envs: Map<string, string>;
  pods: Array<K8sPodState>;
  memoryLimit: MemoryLimit;
  memoryRequest: MemoryRequest;
  replicaCount: number;
  status: Status;
}

export interface K8sPodState {
  name: string;
  phase?: string;
  startTime?: string;
  restartCount?: number;
}

export enum LogType {
  INCR_DEPLOY_STATUS_CHANGE = "incrdeploy-change",
  DATAX_WORKER_POD_LOG = "datax-worker-pod-log"
}

export interface RcHpaStatus {
  conditions: Array<HpaConditionEvent>;
  currentMetrics: Array<HpaMetrics>;
  autoscalerStatus: HpaAutoscalerStatus;
  autoscalerSpec: HpaAutoscalerSpec;
}

export interface HpaConditionEvent {
  type: string;
  status: string;
  lastTransitionTime: string;
  reason: string;
  message: string;
}

export interface HpaAutoscalerStatus {
  currentCPUUtilizationPercentage: number;
  currentReplicas: number;
  desiredReplicas: number;
  lastScaleTime: number;
}

export interface HpaAutoscalerSpec {
  maxReplicas: number;
  minReplicas: number;
  targetCPUUtilizationPercentage: number;
}

export interface HpaMetrics {
  type: string;

  resource: UsingResource;
}

export interface UsingResource {
  name: string;
  currentAverageUtilization: any;
  currentAverageValue: any;
}

export class K8SControllerStatus {
  public k8sReplicationControllerCreated: boolean;

  public state: "NONE" | "STOPED" | "RUNNING" | "DISAPPEAR";
  // 由于本地执行器没有安装，导致datax执行器无法执行
  public installLocal: boolean;
  public rcDeployment: RCDeployment;
}

export interface PluginExtraProps {
  endType: string;
  extendPoint: string;
  supportIncr: boolean;
  impl: string;
  displayName: string;
}

export interface IncrDesc extends PluginExtraProps {
  extendSelectedTabProp: boolean;
}

export class IndexIncrStatus extends K8SControllerStatus {
  public incrScriptCreated: boolean;
  public incrScriptMainFileContent: string;
  public k8sPluginInitialized: boolean;
  public flinkJobDetail: FlinkJobDetail;
  public incrProcess: IncrProcess;

  public incrSourceDesc: IncrDesc;
  public incrSinkDesc: IncrDesc;
  public readerDesc: PluginExtraProps;
  public writerDesc: PluginExtraProps;


  public static getIncrStatusThenEnter(basicForm: BasicFormComponent, hander: ((r: IndexIncrStatus) => void), cache = true) {
    basicForm.httpPost('/coredefine/corenodemanage.ajax'
      , `action=core_action&emethod=get_incr_status&cache=${cache}`)
      .then((r) => {
        if (r.success) {
          let incrStatus: IndexIncrStatus = Object.assign(new IndexIncrStatus(), r.bizresult);
          hander(incrStatus);
        }
      });
  }

  /**
   * 增量处理节点启动有异常
   */
  public get incrProcessLaunchHasError(): boolean {
    return this.k8sReplicationControllerCreated && this.incrProcess && !this.incrProcess.incrGoingOn && !!this.rcDeployment && !!this.rcDeployment.status && this.rcDeployment.status.readyReplicas > 0;
  }

  public getFirstPod(): K8sPodState {
    for (let i = 0; i < this.rcDeployment.pods.length; i++) {
      return this.rcDeployment.pods[i];
      break;
    }
    return null;
  }
}

export interface IncrProcess {
  incrGoingOn: boolean;
  incrProcessPaused: boolean;
}

export interface ProcessMeta {
  targetName: string;
  pageHeader: string;
  stepsType: StepType
  supportK8SReplicsSpecSetter: boolean;
  // step0
  notCreateTips: string;
  createButtonLabel: string;
}

export class DataXJobWorkerStatus extends K8SControllerStatus {
  processMeta: ProcessMeta;
}

export class DataxWorkerDTO {
  rcSpec: K8SRCSpec;
  processMeta: ProcessMeta;
}
