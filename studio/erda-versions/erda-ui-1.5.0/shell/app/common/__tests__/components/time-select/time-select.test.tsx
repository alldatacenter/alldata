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
import TimeSelect, { IProps, TimeRange } from 'common/components/time-select/time-select';
import { relativeTimeRange } from 'common/components/time-select/common';
import { mount, ReactWrapper } from 'enzyme';
import moment from 'moment';
import { act } from 'react-dom/test-utils';

describe('time-select', () => {
  describe('TimeSelect', () => {
    const renderTimeSelect = (props: IProps): ReactWrapper<IProps, {}, React.Component> => {
      return mount(<TimeSelect {...props} />);
    };
    it('TimeSelect should trigger change when mounted', () => {
      const changeFn = jest.fn();
      const wrapper = renderTimeSelect({
        onChange: changeFn,
        triggerChangeOnMounted: true,
        defaultValue: { mode: 'quick', quick: 'days:1', customize: {} },
      });
      expect(wrapper.find('.time-range').text()).toBe('last 1 days');
      expect(changeFn).toHaveBeenCalledTimes(1);
      wrapper.find('.manual-refresh').children().simulate('click');
      expect(changeFn).toHaveBeenCalledTimes(2);
    });
    it('TimeSelect manual refresh should work well', () => {
      const changeFn = jest.fn();
      const wrapper = renderTimeSelect({ onChange: changeFn });
      wrapper.find('.manual-refresh').children().simulate('click');
      expect(changeFn).toHaveBeenCalledTimes(1);
    });
    it('TimeSelect open DropDrown should work well', () => {
      const wrapper = renderTimeSelect({ className: 'custom-name' });
      expect(wrapper.find('.time-select')).toHaveClassName('custom-name');
      expect(wrapper.find('Dropdown').prop('visible')).toBeFalsy();
      wrapper.find('.time-range').simulate('click');
      expect(wrapper.find('Dropdown').prop('visible')).toBeTruthy();
    });
    it('TimeSelect should auto refresh', () => {
      jest.useFakeTimers();
      const changeFn = jest.fn();
      const wrapper = renderTimeSelect({ strategy: 'seconds:5', onChange: changeFn });
      jest.advanceTimersByTime(6000);
      expect(changeFn).toHaveBeenCalledTimes(1);
      wrapper.setProps({
        strategy: 'off',
      });
      jest.advanceTimersByTime(6000);
      expect(changeFn).toHaveBeenCalledTimes(1);
      wrapper.unmount();
    });
    it('TimeSelect Child Comp method should work well', () => {
      const changeFn = jest.fn();
      const changeStrategyFn = jest.fn();
      const wrapper = renderTimeSelect({ onChange: changeFn, onRefreshStrategyChange: changeStrategyFn });
      act(() => {
        wrapper.find('AutoRefreshStrategy').prop('onChange')?.('seconds:5');
      });
      expect(changeStrategyFn).toHaveBeenCalledWith('seconds:5');
      wrapper.find('.time-range').simulate('click');
      expect(wrapper.find('TimeRange')).toHaveLength(1);
      act(() => {
        wrapper.find('TimeRange').prop('onChange')?.({ mode: 'quick', quick: 'days:1', customize: {} });
      });
      expect(changeFn).toHaveBeenCalledTimes(1);
    });
  });
  describe('TimeRange', () => {
    it('TimeRange should work well', () => {
      const changeFn = jest.fn();
      const wrapper = mount(<TimeRange onChange={changeFn} />);
      expect(wrapper.find('.time-quick-select-item')).toHaveLength(Object.keys(relativeTimeRange).length);
      wrapper.find('.time-quick-select-item').at(0).simulate('click');
      expect(changeFn).toHaveBeenLastCalledWith({
        mode: 'quick',
        quick: Object.keys(relativeTimeRange)[0],
        customize: {
          start: undefined,
          end: undefined,
        },
      });
      expect(wrapper.find('.time-quick-select-item').at(0)).toHaveClassName('text-primary');
      const date = moment();
      act(() => {
        wrapper.find('Picker').at(0).prop('onChange')?.(date);
      });
      act(() => {
        wrapper.find('Picker').at(2).prop('onChange')?.(date);
      });
      expect(changeFn).toHaveBeenLastCalledWith({
        mode: 'customize',
        quick: undefined,
        customize: {
          start: date,
          end: date,
        },
      });
    });
  });
});
