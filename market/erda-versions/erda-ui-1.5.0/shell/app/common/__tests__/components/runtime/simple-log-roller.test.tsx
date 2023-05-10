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
import { SimpleLog } from 'common';
import { LogItem } from 'common/components/simple-log/simple-log-roller';
import { mount, shallow } from 'enzyme';
import commonStore from 'common/stores/common';

const logsMap = {
  common: {
    content: [
      {
        source: 'log content',
        id: 'log-erda-2021-06-29',
        stream: 'stdout',
        timeBucket: '1624924800000000000',
        timestamp: 1624954155350947968,
        offset: '408',
        content: '2021-06-29T11:05:45.713Z INFO - [erda-log-content]',
        level: 'INFO',
        requestId: 'r-2021-06-29',
      },
    ],
    emptyTimes: 1,
    fetchPeriod: 500,
  },
};

describe('simple-log-roller', () => {
  beforeAll(() => {
    jest.mock('common/stores/common');
    commonStore.useStore = (fn) => {
      return fn({ logsMap });
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('SimpleLog.Roller work well', () => {
    const fetchLogFn = jest.fn().mockResolvedValue();
    const clearLogFn = jest.fn();
    commonStore.effects.fetchLog = fetchLogFn;
    commonStore.reducers.clearLog = clearLogFn;
    const wrapper = mount(
      <SimpleLog.Roller
        logKey="log-erda"
        style={{ color: '#ff0000' }}
        query={{
          requestId: 'r-2021-06-29',
        }}
      />,
    );
    expect(fetchLogFn).toHaveBeenLastCalledWith({ logKey: 'log-erda', requestId: 'r-2021-06-29' });
    wrapper.setProps({
      query: {
        requestId: 'r-2021-06-29',
        applicationId: 'erda-001',
      },
    });
    expect(clearLogFn).toHaveBeenCalled();
    expect(fetchLogFn).toHaveBeenLastCalledWith({
      logKey: 'log-erda',
      requestId: 'r-2021-06-29',
      applicationId: 'erda-001',
    });
  });
  it('LogItem should work well ', () => {
    const wrapper = shallow(<LogItem log={logsMap.common.content[0]} />);
    expect(wrapper.find('.log-item-logtime').text()).toBe('2021-06-29T11:05:45.713Z');
    expect(wrapper.find('.log-item-level').text()).toBe('INFO');
    expect(wrapper.find('.log-item-content').prop('dangerouslySetInnerHTML')).toStrictEqual({
      __html: '[erda-log-content] --- ',
    });
  });
});
