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

/* eslint-disable react-hooks/exhaustive-deps */
import React from 'react';
import { last, get } from 'lodash';
import { Cascader } from 'antd';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';

interface IProps {
  type: string;
  api: string;
  query: object;
  viewProps?: object;
  onChange?: (args: any) => void;
  dataHandler?: (arg: any) => any;
}

const ModuleSelector = ({ type, api, dataHandler, query, onChange, viewProps }: IProps) => {
  const [modules, timeSpan, chosenApp] = monitorCommonStore.useStore((s) => [
    s.modules,
    s.globalTimeSelectSpan.range,
    s.chosenApp,
  ]);
  const { getModules } = monitorCommonStore.effects;
  const { clearModules, changeChosenModules } = monitorCommonStore.reducers;

  useEffectOnce(() => {
    onGetModules();
    return () => {
      clearModules({ type });
    };
  });

  React.useEffect(() => {
    const { startTimeMs, endTimeMs } = timeSpan;
    const filter_application_id = get(chosenApp, 'id');
    onGetModules({ start: startTimeMs, end: endTimeMs, filter_application_id });
  }, [timeSpan, chosenApp]);

  const onGetModules = (extendQuery?: any) => {
    const { startTimeMs, endTimeMs } = timeSpan;
    const filter_application_id = get(chosenApp, 'id');
    const finalQuery = { ...query, start: startTimeMs, end: endTimeMs, filter_application_id, ...extendQuery };
    finalQuery.filter_application_id && getModules({ api, query: finalQuery, dataHandler, type }); // 有appId才发起请求
  };

  const displayRender = (label: string[]) => {
    if (!(label[1] && label[2])) return '';
    return `${label[1]} / ${label[2]}`;
  };
  const onChangeChosenModules = (val: string[]) => {
    changeChosenModules({ chosenModule: last(val), type });
  };
  const module = modules[type];
  return (
    <Cascader
      options={module}
      displayRender={displayRender}
      expandTrigger="hover"
      className="condition-selector"
      onChange={onChange || onChangeChosenModules}
      placeholder={i18n.t('msp:please select module')}
      {...viewProps}
    />
  );
};

export default ModuleSelector;
