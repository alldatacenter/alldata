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

declare namespace AUTO_TEST {
  interface ICaseDetail {
    type: string;
    inode: string; // 节点id
    pinode: string; // 父节点id
    name: string;
    desc: string;
    scope: string;
    scopeID: string;
    creatorID: string;
    updaterID: string;
    createdAt: string;
    updatedAt: string;
    meta: {
      [pro: string]: any;
      pipelineYml: string;
      snippetAction: {
        [pro: string]: any;
        snippet_config: {
          name: string;
          source: string;
        };
      };
    };
  }

  interface ICaseTreeQuery {
    scopeID: string;
    scope: string;
    pinode: string;
  }

  interface IConfigEnv {
    desc: string;
    ns: string;
    displayName: string;
  }

  interface IConfigEnvQuery {
    scopeID: string;
    scope: string;
  }

  interface IRunRecordQuery extends IPagingReq {
    ymlNames: string;
    sources: string;
  }

  interface ISnippetConfig {
    source: string;
    name: string;
    labels?: Obj;
    alias: string;
  }

  interface ISnippetDetailQuery {
    snippetConfigs: ISnippetConfig[];
  }

  interface ISnippetDetail {
    key: string;
    value: string;
    params: PIPELINE.IPipelineInParams;
    outputs: string[];
  }

  interface ISnippetDetailRes {
    [pro: string]: ISnippetDetail;
  }

  interface ICreateAndRunQuery {
    pipelineYml: string;
    pipelineSource: string;
    pipelineYmlName: string;
    clusterName: string;
    configManageNamespaces?: string[];
    autoRunAtOnce: boolean;
    autoStartCron: boolean;
    runParams?: Array<{ name: string; value: string }>;
    labels?: { orgID: string; projectID: string };
  }

  interface IUpdateCaseBody {
    nodeId: string;
    pipelineYml?: string;
    runParams?: Array<{ value: string; name: string }>;
  }
}
