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

declare namespace COMMON_STRATEGY_NOTIFY {
  interface IRule {
    alertIndex: string;
    window: number;
    functions: Array<{
      field: string;
      aggregator: string;
      operator: string;
      value: any;
    }>;
    isRecover: boolean;
  }

  interface IPageParam {
    pageNo?: number;
    pageSize?: number;
  }

  interface IKeyDisplay {
    key: string;
    display: string;
  }

  interface IAlert {
    id: number;
    name: string;
    enable: boolean;
    clusterNames?: string[];
    appIds?: string[];
    notifies: INotifyGroupNotify[];
    createTime: number;
  }

  interface INotifyGroup {
    id: number;
    name: string;
    scopeType: string;
    scopeId: string;
    targets: INotifyTarget[];
    creatorId: string;
  }

  interface INotifyGroupNotify {
    id: number;
    type: 'notify_group';
    groupId: string;
    groupType: string;
    notifyGroup?: INotifyGroup;
    dingdingUrl?: string;
  }

  interface INotifyTarget {
    type: string;
    values: Array<{
      receiver: string;
      secret: string;
    }>;
  }

  interface IAlertBody {
    name: string;
    clusterNames?: string[];
    appIds?: string[];
    domain: string;
    rules: IRule[];
    notifies: Array<{
      silence: {
        value: number;
        unit: string;
        policy: string;
      };
      type: string;
      groupId: number;
      groupType: string;
      level: string;
    }>;
  }

  interface IAlertDetail {
    rules: IRule[];
  }

  interface IDataExpression {
    id: number;
    alertIndex: IKeyDisplay;
    window: number;
    functions: Array<{
      field: IKeyDisplay;
      aggregator: string;
      operator: string;
      dataType: string;
      value: any;
    }>;
    isRecover: boolean;
  }

  interface IAlertTypeRule {
    alertType: IKeyDisplay;
    rules: IDataExpression[];
  }

  interface ISlience {
    value: number;
    unit: {
      key: string;
      display: string;
    };
  }

  interface IFormRule {
    key: string;
    alertIndex: string;
    window: number;
    functions: Array<{
      field: string;
      aggregator: string;
      operator: string;
      dataType: string;
      value: any;
    }>;
    isRecover: boolean;
  }

  interface IAlertType {
    alertTypeRules: IAlertTypeRule[];
    windows: number[];
    operators: IKeyDisplay[];
    aggregator: IKeyDisplay[];
    silence: ISlience[];
  }

  interface IAlertTriggerCondition {
    key: string;
    index: string;
    filters: string[];
    displayName: string;
  }

  interface IAlertTriggerConditionContent {
    key: string;
    options: string[];
  }

  interface IAlertTriggerConditionQueryItem {
    condition: string;
    filters: object;
    index: string;
  }
}
