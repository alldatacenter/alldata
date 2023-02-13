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

import { createStore } from 'core/cube';

interface IState {
  infoMap: {
    publisherName: string;
    projectName: string;
    appName: string;
    runtimeName: string;
    monitorStatusDetail: Obj;
    SIBaseInfo: Obj;
    opReportName: string;
    publishModuleName: string;
    dashboardName: string;
    alarmRecordName: string;
    assetName: string;
    clientName: string;
    iterationName: string;
    k8sName: string;
    testSpaceName: string;
    curOrgName: string;
    mspProjectName: string;
    cmpCluster: null | ORG_CLUSTER.ICluster;
  };
}

const initState: IState = {
  infoMap: {
    publisherName: '',
    projectName: '',
    appName: '',
    runtimeName: '',
    monitorStatusDetail: {},
    SIBaseInfo: {},
    publishModuleName: '',
    opReportName: '',
    dashboardName: '',
    alarmRecordName: '',
    assetName: '',
    clientName: '',
    iterationName: '',
    k8sName: '',
    testSpaceName: '',
    curOrgName: '',
    mspProjectName: '',
    cmpCluster: null,
  },
};

type Key = keyof typeof initState.infoMap;

const breadcrumb = createStore({
  name: 'breadcrumb',
  state: initState,
  reducers: {
    setInfo(state, key: Key, value: any) {
      state.infoMap[key] = value;
    },
  },
});

export default breadcrumb;
