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
import { shallow } from 'enzyme';
import { LogRoller } from 'common';
import { LogRoller as LogRollerChild } from 'common/containers/log-roller';

const fetchPeriod = 500;
const logKey = 'runtime';

describe('LogRoller', () => {
  beforeAll(() => {
    jest.mock('common/stores/common');
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('LogRoller should render well', () => {
    const fetchLogFn = jest.fn().mockResolvedValue({});
    const clearLog = jest.fn();
    jest.useFakeTimers();
    const wrapper = shallow(
      <LogRollerChild fetchPeriod={fetchPeriod} fetchLog={fetchLogFn} logKey={logKey} clearLog={clearLog} />,
    );
    wrapper.instance().rollingTimeout = 1;
    wrapper.instance().logRoller = {
      preElm: {
        scrollHeight: 1000,
      },
    };
    wrapper.instance().componentDidMount();
    jest.runAllTimers();
    expect(fetchLogFn).toHaveBeenCalled();

    wrapper.setProps({
      requestId: '123',
      pause: true,
    });
    expect(wrapper.instance().state.rolling).toBeFalsy();
    expect(wrapper.instance().rollingTimeout).toBeUndefined();
    wrapper.setProps({
      pause: false,
    });
    expect(wrapper.instance().state.rolling).toBeTruthy();
    expect(wrapper.instance().rollingTimeout).toBe(-1);
    wrapper.instance().goToTop();
    expect(fetchLogFn).toHaveBeenCalled();
    wrapper.unmount();
    expect(clearLog).toHaveBeenLastCalledWith(logKey);
  });
});
