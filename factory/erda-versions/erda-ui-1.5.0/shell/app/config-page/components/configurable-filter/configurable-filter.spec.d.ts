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

declare namespace CP_CONFIGURABLE_FILTER {
  interface Spec {
    type: 'ConfigurableFilter';
    data: Data;
    props?: IProps;
    state: IState;
  }

  interface Data {
    conditions: Condition[];
    filterSet: ConfigItem[];
  }

  interface IState {
    values: Obj;
    selectedFilterSet: number | string;
  }

  interface IProps {
    processField: (field: Obj) => Obj;
  }

  interface ConfigItem {
    id: number | string;
    label: string;
    values: Obj;
    isPreset?: boolean;
  }

  interface Condition {
    key: string;
    label: string;
    type: ConditionType;
    emptyText?: string;
    value: string | number;
    fixed?: boolean;
    showIndex?: number; // 0： 隐藏、其他显示
    haveFilter?: boolean;
    placeholder?: string;
    quickSelect?: IQuickSelect;
    options?: IOption[];
  }

  interface IOption {
    label: string;
    value: string | number;
    icon?: string;
    status?: string;
    children?: IOption[];
  }

  interface IQuickSelect {
    label: string;
    operationKey: string;
  }

  type ConditionType = 'select' | 'input' | 'dateRange';

  type Props = MakeProps<Spec>;
}
