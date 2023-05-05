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
import { CustomFilter } from 'common';
import { useFilter } from 'common/use-hooks';
import { mount } from 'enzyme';
import { DatePicker, Input, Select } from 'antd';
import _ from 'lodash';
import { act } from 'react-dom/test-utils';
import routeInfoStore from 'core/stores/route';
import { getTimeRanges } from 'common/utils';
import { createBrowserHistory } from 'history';
import { setConfig } from 'core/config';

const { RangePicker } = DatePicker;
const filterConfig = [
  {
    type: Input,
    name: 'keyword',
    customProps: {
      className: 'keyword-field',
      placeholder: 'search by keywords',
      autoComplete: 'off',
    },
  },
  {
    type: Select,
    name: 'IP',
    customProps: {
      className: 'ip-field',
      placeholder: 'search by IP',
      autoComplete: 'off',
    },
  },
  {
    type: Input.TextArea,
    name: 'desc',
    customProps: {
      className: 'desc-field',
      placeholder: 'search by desc',
      autoComplete: 'off',
    },
  },
  {
    type: RangePicker,
    name: 'timeStart,timeEnd',
    valueType: 'range',
    customProps: {
      className: 'time-field',
      showTime: true,
      allowClear: false,
      format: 'MM-DD HH:mm',
      style: { width: 'auto' },
      ranges: getTimeRanges(), // 因为选择预设range时不触发onOk，所以onChange时直接触发
    },
  },
];

const updateRouter = (search: string = '') => {
  routeInfoStore.reducers.$_updateRouteInfo(
    {
      pathname: '/erda/dop/apps/123',
      search,
    },
    {
      routePatterns: ['/erda/dop/apps/:id'],
      routeMap: {
        '/erda/dop/apps/:id': {
          route: {
            path: '/erda/dop/apps/123',
          },
        },
      },
    },
  );
};

const timeStart = 1625020000000;
const timeEnd = 1625120680000;

describe('custom-filter', () => {
  beforeAll(() => {
    jest.mock('lodash');
    _.debounce = (fn: Function) => fn;
    const browserHistory = createBrowserHistory();
    setConfig('history', browserHistory);
  });
  afterAll(() => {
    jest.resetAllMocks();
    setConfig('history', undefined);
  });
  it('useFilter should work well', () => {
    jest.useFakeTimers();
    updateRouter(`?keyword=erda&timeStart=${timeStart}&timeEnd=${timeEnd}`);
    const getData = jest.fn();
    const Comp = () => {
      const { onSubmit, onReset, autoPagination } = useFilter({
        getData,
        requiredKeys: ['keyword'],
      });
      return (
        <div>
          <div className="autoPagination" autoPagination={autoPagination} />
          <CustomFilter isConnectQuery config={filterConfig} onSubmit={onSubmit} onReset={onReset} />
        </div>
      );
    };
    const wrapper = mount(<Comp />);
    act(() => {
      wrapper.find('.erda-filter>.erda-custom-filter').prop('onSubmit')();
    });
    expect(getData).toHaveBeenLastCalledWith({
      keyword: 'erda',
      pageNo: 1,
      pageSize: 15,
      timeStart: `${timeStart}`,
      timeEnd: `${timeEnd}`,
    });
    expect(getData).toHaveBeenCalledTimes(1);
    // act(() => {
    //   wrapper.find('Input.keyword-field').prop('onChange')({ target: { value: 'ERDA' } });
    //   wrapper.find('.ip-field').at(1).prop('onChange')('127.0.0.1');
    //   wrapper.find('.desc-field').at(1).prop('onChange')('hello, erda.cloud');
    // });
    // expect(getData).toHaveBeenCalledTimes(2);
    act(() => {
      updateRouter();
    });
    jest.runAllTimers();
    expect(wrapper.find('CustomFilter').prop('config')).toHaveLength(filterConfig.length);
  });
});
