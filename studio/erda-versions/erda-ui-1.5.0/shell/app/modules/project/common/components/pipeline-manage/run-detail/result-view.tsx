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

import React from 'react';
import { Drawer } from 'antd';
import { get, find, compact, map, isEmpty } from 'lodash';
import i18n from 'i18n';

import './result-view.scss';

interface IProps {
  data: any;
}

const labelMap = {
  api_request: i18n.t('request detail'),
  api_response: i18n.t('response detail'),
  api_assert_detail: i18n.t('dop:assertion detail'),
  api_assert_success: i18n.t('dop:assertion result'),
  status: i18n.t('status'),
  result: i18n.t('result'),
};

export const ResultView = (props: IProps) => {
  const { data } = props;
  const metadata = get(data, 'result.metadata') || [];
  const api_request = find(metadata, { name: 'api_request' });
  const api_response = find(metadata, { name: 'api_response' });
  const api_assert_detail = find(metadata, { name: 'api_assert_detail' });
  const api_assert_success = find(metadata, { name: 'api_assert_success' });

  const status = find(metadata, { name: 'status' });
  const result = find(metadata, { name: 'result' });
  const resArr = compact([api_request, api_response, api_assert_detail, api_assert_success, status, result]) as any[];

  return (
    <div className="test-case-execute-result">
      {map(resArr, (item: any) => {
        let jsonObj = {} as any;
        let jsonStr = item.value;
        try {
          jsonObj = JSON.parse(item.value);
          if (item.name === 'api_response' && jsonObj.body) {
            try {
              jsonObj = {
                ...jsonObj,
                body: JSON.parse(jsonObj.body),
              };
            } catch (e) {
              jsonObj = { ...jsonObj };
            }
          }
        } catch (e) {
          jsonStr = item.value;
        }
        if (!isEmpty(jsonObj)) {
          jsonStr = JSON.stringify(jsonObj, null, 2);
        }
        // const jsonObj = JSON.parse(item.value);
        // const jsonStr = JSON.stringify(item.value, null, 2);
        return (
          <div className="test-case-execute-result-item mb-3" key={item.name}>
            <div className="label">{labelMap[item.name] || item.name}</div>
            <pre className="value">{jsonStr}</pre>
          </div>
        );
      })}
    </div>
  );
};

interface IResultViewDrawerProps extends IProps {
  visible: boolean;
  onClose: () => void;
}

export const ResultViewDrawer = (props: IResultViewDrawerProps) => {
  const { visible, onClose, data } = props;

  return (
    <Drawer width={800} title={i18n.t('dop:execute result')} visible={visible} onClose={onClose}>
      <ResultView data={data} />
    </Drawer>
  );
};
