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

// 可选的action类型
interface IStageAction {
  id: number;
  desc: string;
  displayName: string;
  logoUrl: string;
  name: string;
  type: string;
  public: boolean;
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
}

interface IPipelineYmlStructure {
  cron: string;
  envs: object;
  needUpgrade: boolean;
  stages: IStageTask[][];
  ymlContent?: string;
  version: string;
  description: string;
}
