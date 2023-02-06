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

declare namespace CP_API_EDITOR {
  interface Spec {
    type: 'APIEditor';
    props: IProps;
    state: IState;
    data: IData;
    operations: Obj<CP_COMMON.Operation>;
  }

  interface IAssert {
    comparisonOperators: IComparisonOperators[];
  }

  interface IComparisonOperators {
    label: string;
    value: string;
    allowEmpty: boolean;
  }

  interface IBody {
    form: ICommonAttr;
  }

  interface ICommonAttr {
    showTitle: boolean;
  }

  interface ICommonTemp {
    target: string[];
    temp: Obj[];
  }

  interface IApiExecute {
    text: string;
    type: string;
    allowSave: boolean;
    disabled: boolean;
    menu: IApiExecuteMenu[];
  }

  interface IApiExecuteMenu {
    text: string;
    key: string;
    operations: Obj<CP_COMMON.Operation>;
  }

  interface IProps {
    showSave: boolean;
    index: string;
    visible?: boolean;
    executingMap: Obj;
    asserts: IAssert;
    body: IBody;
    commonTemp: ICommonTemp;
    headers: ICommonAttr;
    methodList: string[];
    params: ICommonTemp;
    apiExecute: IApiExecute;
    loopFormField: Array<import('app/configForm/form/form').FormField>;
  }

  interface IState {
    attemptTest: IStateAttemptTest;
    data: IStateData;
  }

  interface IStateData {
    apiSpecId?: number;
    stepId: number;
    apiSpec: API;
    loop?: ILoop;
  }

  interface ILoop {
    break?: string;
    strategy?: {
      max_times: number;
      decline_ratio: number;
      decline_limit_sec: number;
      interval_sec: number;
    };
  }

  interface IStateAttemptTest {
    status: 'Passed' | 'Failed';
    data: IAttemptTestData;
  }

  interface IAttemptTestData {
    asserts?: ITestDataAsserts;
    response?: ITestDataResponse;
    request?: ITestDataRequest;
  }

  interface ITestDataAsserts {
    success: boolean;
    result: IAssertsResult[];
  }

  interface IAssertsResult {
    arg: string;
    operator: string;
    value: string;
    success: boolean;
    actualValue: string;
    errorInfo: string;
  }

  interface ITestDataResponse {
    status: number;
    headers: Obj;
    body: Obj | string;
  }

  interface ITestDataRequest {
    method: string;
    url: string;
    params: Obj;
    headers: Obj;
    body: IApiBody;
  }

  interface IData {
    marketApiList: IMarketApi[];
  }

  interface IMarketApi {
    id: number;
    path: string;
    description: string;
    version: string;
    method: string;
    url: string;
  }

  interface API {
    headers: Row[];
    method: string;
    url: string;
    name: string;
    params: Row[];
    body: IApiBody;
    out_params: OutParam[];
    asserts: Assert[];
  }

  interface IApiBody {
    type: string;
    content: string | Obj;
  }

  interface Row {
    key: string;
    value: string | number;
    desc: string;
  }

  interface OutParam {
    key: string;
    source: string;
    expression: string;
    matchIndex?: string;
  }

  interface Assert {
    arg: string;
    operator: string;
    value: string;
  }

  type Props = MakeProps<Spec>;
}
