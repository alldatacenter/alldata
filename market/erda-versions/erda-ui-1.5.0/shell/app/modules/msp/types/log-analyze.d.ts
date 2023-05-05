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

declare namespace LOG_ANALYZE {
  // 1、key节点，即没有指定value，也没有枚举values，代表需要用户输入。
  // 2、key+values节点，指定了 key，并枚举了values。
  // 3、key+value节点，指定了key和value。
  interface IDynamicChildren {
    key: string;
    dimension: string;
    dynamic_children?: IDynamicChildren;
  }
  interface TagsTree {
    tag: {
      name: string;
      key: string;
      value?: string;
    };
    children?: TagsTree[];
    dynamic_children?: IDynamicChildren;
  }

  interface GetLogQuery {
    [k: string]: string | number | string[] | undefined;
    start: number;
    end: number;
    query?: string;
  }

  interface Log {
    offset: number;
    content: string;
    source: string;
    tags: { [k: string]: string };
    timestamp: string;
  }

  interface AddonSearchQuery {
    // 作为addon，在微服务中的查询参数
    [k: string]: any;
    clusterName: string;
    start: number;
    end: number;
    query?: string;
    size: number;
    version?: string;
    'tags.dice_application_name'?: string;
    'tags.dice_service_name'?: string;
  }

  interface LogStatistics extends IChartResult {
    title: string;
    total: number;
    interval: number;
  }

  interface Scope {
    scope: string;
    scopeID: string | number;
  }

  interface RuleListItem {
    create_time: string;
    enable: boolean;
    id: number;
    metric: string;
    name: string;
    org_id: number;
    org_name: string;
    update_time: number;
    types: string;
  }

  interface Processor {
    type: string;
    config: {
      pattern: string;
      keys: Array<{
        key: string;
        name: string;
        type: string;
        uint: string;
      }>;
    };
  }

  interface Rule {
    id?: number;
    enable?: boolean;
    metric?: string;
    org_id?: number;
    org_name?: string;
    name: string;
    update_time?: number;
    create_time?: number;
    filters: Array<{
      key: string;
      value: string;
    }>;
    processors: Processor[];
  }

  interface Template {
    content?: string;
    filters: Array<{
      key: string;
      value: string;
    }>;
    name: string;
    processors: Processor[];
  }

  interface RuleTemplate {
    name: string;
    desc: string;
  }

  interface TestRuleQuery {
    content: string;
    name: string;
    processors: Processor[];
  }

  interface TestRuleResp {
    fields: { [k: string]: any };
  }
}
