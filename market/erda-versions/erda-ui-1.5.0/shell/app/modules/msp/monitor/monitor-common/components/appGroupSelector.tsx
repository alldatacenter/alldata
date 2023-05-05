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
import { Cascader } from 'antd';
import { get, isFunction, isEmpty, find } from 'lodash';
import { cutStr } from 'common/utils';
import monitorCommonStore from 'common/stores/monitorCommon';
import { useEffectOnce } from 'react-use';
import i18n from 'i18n';

interface IProps {
  type: string;
  viewProps?: object;
  api: string;
  dataHandler: Function | undefined;
}

const AppGroupSelector = ({ api, type, dataHandler, viewProps }: IProps) => {
  const [appGroup, timeSpan, chosenApp, chosenAppGroup, lastChosenAppGroup] = monitorCommonStore.useStore((s) => [
    s.appGroup,
    s.globalTimeSelectSpan.range,
    s.chosenApp,
    s.chosenAppGroup,
    s.lastChosenAppGroup,
  ]);
  const { getAppGroup } = monitorCommonStore.effects;
  const { changeChosenAppGroup, clearAppGroup } = monitorCommonStore.reducers;

  useEffectOnce(() => {
    getGroup();
    return () => {
      clearAppGroup({ type });
    };
  });

  React.useEffect(() => {
    getGroup({ timeSpan, chosenApp });
  }, [timeSpan, chosenApp]);

  const onChangeChosenAppGroup = (val: string[]) => {
    changeChosenAppGroup({ chosenAppGroup: val, type });
  };

  // 获取上次选中的group，是否在当前group总数据内，若有，则默认选中上次选择
  const getLastChosenMatch = (_appGroup: any[], _type: string, lastGroup: any[] | undefined) => {
    if (isEmpty(lastGroup)) return undefined;
    const curAppGroup = _appGroup[_type];
    const curGroup = curAppGroup && curAppGroup.data;

    const [chosen1, chosen2]: any[] = lastGroup || [];
    const matchChosen1 = find(curGroup, (g) => g.value === chosen1);
    let val;
    // lastChoosenAppGroup值可能为[runtime]或[runtime,service]；
    // 若有runtime,再匹配该runtime下的children(service)，若无runtime，则不再匹配
    if (matchChosen1) {
      val = [matchChosen1.value];
      const matchChosen2 = find(matchChosen1.children, (g) => g.value === chosen2);
      if (matchChosen2) {
        val.push(matchChosen2.value);
      }
    }
    return val;
  };

  const displayRender = (label: string[]) => {
    if (label.length > 1) {
      return `${cutStr(label[0], 8)} / ${cutStr(label[1], 8)}`;
    } else {
      return label;
    }
  };

  const getGroup = (extendQuery = {}) => {
    const totalQuery = { timeSpan, chosenApp, ...extendQuery } as any;
    let finalQuery = {};
    const filter_application_id = get(chosenApp, 'id');
    if (isFunction(totalQuery.query)) {
      finalQuery = totalQuery.query({ timeSpan, chosenApp });
    } else {
      const { startTimeMs, endTimeMs } = timeSpan;
      finalQuery = { ...totalQuery.query, start: startTimeMs, end: endTimeMs, filter_application_id };
    }
    filter_application_id && getAppGroup({ api, query: finalQuery, dataHandler, type }); // 有appId才发起请求
  };

  const data = appGroup[type] || [];
  const lastMatchChosen = getLastChosenMatch(appGroup, type, lastChosenAppGroup);
  const value = chosenAppGroup[type] || lastMatchChosen;

  React.useEffect(() => {
    const loaded = data && data.loading === false;
    loaded && changeChosenAppGroup && changeChosenAppGroup(value);
  }, [value]);

  return (
    <Cascader
      options={get(data, 'data')}
      value={value}
      changeOnSelect
      displayRender={displayRender}
      expandTrigger="hover"
      className="condition-selector"
      onChange={onChangeChosenAppGroup}
      placeholder={i18n.t('msp:please choose')}
      {...viewProps}
    />
  );
};

export default AppGroupSelector;
