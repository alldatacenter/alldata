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

declare namespace MS_INDEX {
  type IMspProjectEnv = WORKSPACE | 'DEFAULT'; // check 作为函数，应该返回布尔值

  interface IMspRelationship {
    workspace: IMspProjectEnv;
    tenantId: string;
    displayWorkspace: string;
  }

  interface ICreateProject {
    name: string;
    displayName: string;
    id: string;
    type: 'MSP' | 'DOP';
  }

  interface IMspProject {
    id: number;
    name: string;
    displayName: string;
    isDelete: boolean;
    type: 'MSP' | 'DOP';
    displayType: string;
    relationship: IMspRelationship[];
    createTime: number;
    updateTime: number;
    last24hAlertCount: number;
    desc: string;
    lastActiveTime: number;
    serviceCount: number;
    logo?: string;
  }

  type ISubMenuKey =
    | 'ServiceMonitor'
    | 'FrontMonitor'
    | 'ActiveMonitor'
    | 'AlertStrategy'
    | 'AlarmHistory'
    | 'RuleManagement'
    | 'NotifyGroupManagement'
    | 'Tracing'
    | 'LogAnalyze'
    | 'ErrorInsight'
    | 'Dashboard'
    | 'APIGateway'
    | 'RegisterCenter'
    | 'ConfigCenter'
    | 'AccessConfig'
    | 'MemberManagement'
    | 'ComponentInfo';

  type IRootMenu =
    | 'Overview'
    | 'MonitorCenter'
    | 'AlertCenter'
    | 'DiagnoseAnalyzer'
    | 'ServiceManage'
    | 'EnvironmentSet';

  type IMenuKey = IRootMenu | ISubMenuKey;

  interface IMspMenu<T = IRootMenu> {
    href: string;
    key: T; // menuKey，用于匹配菜单高亮
    clusterName: string;
    clusterType: string;
    cnName: string;
    enName: string;
    exists?: boolean; // false表示没有用到或还未拉起来，先展示引导页
    params: {
      [key: string]: string;
      key: IMenuKey;
      tenantId: string;
      terminusKey: string;
      tenantGroup: string;
    };
    children: IMspMenu<ISubMenuKey>[];
  }

  interface Menu {
    href: string;
    icon?: string;
    key: string;
    text: string;
    prefix: string;
    subMenu?: Menu[];
  }

  interface MenuMap {
    [k: string]: IMspMenu;
  }

  interface IChartMetaData {
    id: string;
    name: string;
    desc: string;
    scope: string;
    scopeId: string;
    viewConfig: import('@erda-ui/dashboard-configurator/dist').Layout;
  }
}
