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

declare namespace CP_COMMON {
  interface Operation {
    [pro: string]: any;
    key?: string;
    reload?: boolean;
    skipRender?: boolean; // skipRender is a new key to replace reload;
    async?: boolean;
    text?: string;
    disabled?: boolean;
    disabledTip?: string;
    confirm?: string | IConfirm;
    command?: Command | Command[];
    meta?: Obj;
    show?: boolean;
    successMsg?: string;
    errorMsg?: string;
    prefixIcon?: string;
    fillMeta?: string;
    showIndex?: number;
    serverData?: Obj;
    clientData?: Obj;
  }

  interface IConfirm {
    title: string;
    subTitle: string;
  }

  interface Command {
    [pro: string]: any;
    key: string;
    target?: string;
    state?: Obj;
    jumpOut?: boolean;
  }

  interface FormField {
    label: string; // 标签
    key: string; // 字段名，唯一的key，支持嵌套
    component: string;
    group?: string;
    labelTip?: string;
    defaultValue?: Obj | number | string; // 重置时不会清掉
    initialValue?: Obj; // 仅在mount后set，重置会清掉
    rules?: Obj[];
    required?: boolean;
    componentProps?: Obj;
    removeWhen?: Array<Array<{ field: string; operator: string; value: any }>>;
    disableWhen?: Array<Array<{ [prop: string]: any }>>;
  }
}
