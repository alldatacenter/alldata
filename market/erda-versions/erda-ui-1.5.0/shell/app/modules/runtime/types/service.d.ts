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

declare namespace RUNTIME_SERVICE {
  interface Err {
    ctx: {
      [k: string]: any;
      instanceId: string;
    };
    msg: string;
  }

  interface Detail {
    status: string;
    deployments: { replicas: number };
    resources: CPU_MEM_DISK;
    envs: null | Obj;
    addrs: string[];
    expose: string[];
    errors: null | Err[];
  }

  interface Instance {
    id?: string;
    containerId: string; // k8s集群id为这个
    ipAddress: string;
    host: string;
    image: string;
    cpu: number;
    memory: number;
    disk: number;
    status: string;
    exitCode: number;
    startedAt: string;
    service: string;
    clusterName: string;
    isRunning?: boolean;
    cluster_full_name?: string; // 可能是监控里才有
  }

  type Status = 'running' | 'stopped';

  interface ListQuery {
    runtimeID: number;
    serviceName: string;
    status: Status;
  }

  interface PodQuery {
    runtimeID: number;
    service: string;
  }

  interface KillPodBody {
    runtimeID: number;
    podName: string;
  }

  interface Pod {
    k8sNamespace: string;
    podName: string;
    clusterName: string;
    service: string;
    startedAt: string;
    message: string;
    phase: string;
    host: string;
    ipAddress: string;
    uid: string;
  }

  interface InsMap {
    runs: Instance[];
    completedRuns: Instance[];
  }

  interface UpdateConfigQuery {
    applicationId: number;
    workspace: WORKSPACE;
    runtimeName: string;
  }

  interface PreOverlay {
    services: RUNTIME_SERVICE.PreOverlayService;
  }

  interface PreOverlayService {
    [k: string]: {
      resources: {
        cpu: number;
        mem: number;
        disk: number;
      };
      deployments: {
        replicas: number;
      };
    };
  }

  interface UpdateConfigBody {
    query: RUNTIME_SERVICE.UpdateConfigQuery;
    data: RUNTIME_SERVICE.PreOverlay;
  }

  interface SocketData {
    errors: null | Err[];
    runtimeId: number;
    serviceName: string;
    status: string;
  }
}
