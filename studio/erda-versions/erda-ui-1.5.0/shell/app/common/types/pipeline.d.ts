// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

declare namespace PIPELINE {
  interface IPipelineYmlStructure {
    cron: string;
    envs: object;
    needUpgrade: boolean;
    stages: IStageTask[][];
    outputs: IPipelineOutParams[];
    params: IPipelineInParams[];
    ymlContent?: string;
    version: string;
    description: string;
  }

  interface IStageTask {
    alias: string;
    params?: any;
    commands?: string[];
    type: string;
    version?: string;
    image?: string;
    timeout?: string;
    resources: Obj;
    if?: string;
  }

  interface IStage {
    id: number;
    pipelineID: number;
    name: string;
    status: string;
    costTimeSec: number;
    timeBegin: string;
    timeEnd: string;
    pipelineTasks: ITask[];
  }

  interface ITask {
    id: number;
    pipelineID: number;
    stageID: number;
    name: string;
    type: string;
    status: string;
    costTimeSec: number;
    displayName?: string;
    logoUrl?: string;
    extra: {
      uuid: string;
      allowFailure: boolean;
      taskContainers: ITaskContainers[];
    };
    result: {
      metadata?: MetaData[];
    };
    [k: string]: any;
  }

  interface ITaskContainers {
    taskName: string;
    containerID: string;
  }

  interface IPipelineDetail {
    extra: {
      diceWorkspace: string;
      showMessage?: {
        msg: string;
        stacks: string[];
      };
    };
    id: string;
    status: string;
    source: string;
    env: string;
    branch: string;
    ymlName: string;
    commitId: string;
    costTimeSec: string;
    commit: string;
    commitDetail: {
      author: string;
      time: string;
      comment: string;
    };
    pipelineButton: {
      canManualRun: boolean;
      canCancel: boolean;
      canForceCancel: boolean;
      canRerun: boolean;
      canRerunFailed: boolean;
      canStartCron: boolean;
      canStopCron: boolean;
      canPause: boolean;
      canUnpause: boolean;
      canDelete: boolean;
    };
    pipelineSnippetStages: any[];
    pipelineCron: { [key: string]: any; id: number };
    pipelineStages: IStage[];
    pipelineTaskActionDetails: Obj<ITaskActionDetail>;
    runParams?: IPipelineInParams[];
  }

  interface ITaskActionDetail {
    displayName: string;
    logoUrl: string;
  }

  interface IPipelineInParams {
    name: string;
    default?: any;
    desc?: string;
    inConfig?: boolean;
    required?: boolean;
    type: string;
    value?: any;
  }

  interface IPipelineOutParams {
    name: string;
    desc: string;
    ref: string;
  }

  interface MetaData {
    name: string;
    value: string;
  }

  type IPipeline = Omit<IPipelineDetail, 'pipelineButton'>;
}
