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

declare namespace RESOURCE {
  interface Instances {
    id: string;
    name: string;
    instance: number;
    unhealthy: number;
    memory: number;
    disk: number;
    cpu: number;
  }

  interface ServiceItem {
    containerId: string;
    ipAddress: string;
    host: string;
    image: string;
    cpu: number;
    memory: number;
    disk: number;
    status: string;
    exitCode: number;
    message: string;
    startedAt: string;
    service: string;
    clusterName: string;
  }

  interface RespError {
    success: boolean;
    data: null;
    err: {
      msg: any;
    };
  }

  interface QueryServiceList {
    paths: Array<{
      q: string;
      name: string;
    }>;
    org: number;
    startLevel: string;
  }

  interface QuertChart {
    query: {
      start: number;
      end: number;
    };
    projectId: string;
    type: string;
    startLevel: string;
    paths: Array<{
      q: string;
      name: string;
    }>;
  }

  interface MetaData {
    agg: string;
    axisIndex: number;
    chartType: string;
    data: number[];
    name: string;
    tag: string;
    unit: string;
    unitType: string;
  }

  interface CharData {
    results: Array<{
      data: Array<{
        [k: string]: MetaData;
      }>;
      name: string;
    }>;
    time: number[];
    title: string;
    total: number;
  }
}
