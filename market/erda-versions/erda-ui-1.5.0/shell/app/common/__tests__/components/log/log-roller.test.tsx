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
import { LogRoller } from 'common/components/pure-log-roller/log-roller';
import { mount } from 'enzyme';
import { sleep } from '../../../../../test/utils';

describe('LogRoller', () => {
  const content = '2021-06-29T11:05:45.713Z INFO - [erda-log-content]';
  it('LogRoller should work well', async () => {
    const props = {
      rolling: true,
      hasLogs: true,
      backwardLoading: true,
    };
    const goToTopFn = jest.fn();
    const goToBottomFn = jest.fn();
    const cancelRollingFn = jest.fn();
    const startRollingFn = jest.fn();
    const showDownloadFn = jest.fn();
    const wrapper = mount(
      <LogRoller
        content={content}
        onGoToTop={goToTopFn}
        onGoToBottom={goToBottomFn}
        onCancelRolling={() => {
          props.rolling = false;
          cancelRollingFn();
        }}
        onStartRolling={() => {
          props.rolling = true;
          startRollingFn();
        }}
        onShowDownloadModal={showDownloadFn}
        {...props}
      />,
    );
    // download log
    wrapper.find('Button').at(0).simulate('click');
    expect(showDownloadFn).toHaveBeenCalledTimes(1);
    // full screen
    expect(wrapper.find('Button').at(1).text()).toBe('full screen');
    wrapper.find('Button').at(1).simulate('click');
    expect(wrapper.find('Button').at(1).text()).toBe('exit full screen');
    // go to bottom
    wrapper.find('Button').at(2).simulate('click');
    expect(goToBottomFn).toHaveBeenCalledTimes(1);
    // go to top
    wrapper.find('Button').at(3).simulate('click');
    expect(goToTopFn).toHaveBeenCalledTimes(1);
    // pause / start
    expect(wrapper.find('Button').at(4).text()).toBe('pause');
    wrapper.find('Button').at(4).simulate('click');
    expect(cancelRollingFn).toHaveBeenCalledTimes(1);
    wrapper.setProps(props);
    expect(wrapper.find('Button').at(4).text()).toBe('start');
    wrapper.find('Button').at(4).simulate('click');
    expect(startRollingFn).toHaveBeenCalledTimes(1);
    wrapper.setProps(props);
    expect(wrapper.find('Button').at(4).text()).toBe('pause');
    Object.defineProperty(wrapper.instance(), 'preElm', {
      value: {
        scrollHeight: 200,
        scrollTop: 20,
        clientHeight: 50,
      },
    });
    wrapper.find('.log-content-wrap').simulate('scroll');
    expect(cancelRollingFn).toHaveBeenCalledTimes(2);
    Object.defineProperty(wrapper.instance(), 'preElm', {
      value: {
        scrollHeight: 200,
        scrollTop: 0,
        clientHeight: 50,
      },
    });
    await sleep(1000);
    wrapper.find('.log-content-wrap').simulate('scroll');
    expect(goToTopFn).toHaveBeenCalledTimes(2);
  });
});
