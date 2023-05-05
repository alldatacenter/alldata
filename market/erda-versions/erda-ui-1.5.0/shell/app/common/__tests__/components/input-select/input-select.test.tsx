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
import { InputSelect } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

const options = [
  {
    label: 'DEV',
    value: 'dev',
  },
  {
    label: 'TEST',
    value: 'test',
  },
  {
    label: 'STAGING',
    value: 'staging',
  },
  {
    label: 'PROD',
    value: 'prod',
  },
];

describe('InputSelect', () => {
  it('should render well', () => {
    const onChangeFn = jest.fn();
    const onBlurFn = jest.fn();
    const wrapper = mount(<InputSelect className="input-select-inp" onChange={onChangeFn} onBlur={onBlurFn} />);
    act(() => {
      wrapper.find('Input.input-select-inp').prop('onFocus')(new Event('focus'));
      wrapper.find('Input.input-select-inp').prop('onBlur')();
    });
    expect(onBlurFn).toHaveBeenCalled();
    act(() => {
      wrapper.setProps({
        value: 'erda',
      });
    });
    wrapper.update();
    expect(wrapper.find('Input.input-select-inp').prop('value')).toBe('erda');
    act(() => {
      document.body.dispatchEvent(new Event('click'));
    });
    act(() => {
      wrapper.find('Input.input-select-inp').prop('onChange')({ target: { value: 'new value' } });
    });
    expect(onChangeFn).toHaveBeenLastCalledWith('new value');
    wrapper.unmount();
  });
  it('should render show Select Dropdown', () => {
    const onChangeFn = jest.fn();
    const onBlurFn = jest.fn();
    jest.useFakeTimers();
    const wrapper = mount(
      <InputSelect
        className="input-select-inp"
        onChange={onChangeFn}
        onBlur={onBlurFn}
        value="dev"
        options={options}
      />,
    );
    act(() => {
      wrapper.find('Input.input-select-inp').prop('onFocus')(new Event('focus'));
    });
    wrapper.update();
    expect(wrapper.find('.input-select-dropdown-menu')).toExist();
    expect(wrapper.find('PureSelect')).toExist();
    expect(wrapper.find('.option-item')).toHaveLength(options.length);
    act(() => {
      wrapper.find('.option-item').at(1).simulate('click');
      jest.runAllTimers();
    });
    wrapper.update();
    expect(onChangeFn).toHaveBeenLastCalledWith(options[1].value);
    expect(wrapper.find('.option-item').at(1).prop('className')).toContain('bg-light-active');
    act(() => {
      wrapper
        .find('.input-select-dropdown-box')
        .find('Input')
        .simulate('change', { target: { value: options[0].value } });
    });
    wrapper.update();
    expect(wrapper.find('.option-item')).toHaveLength(1);
    act(() => {
      wrapper
        .find('.input-select-dropdown-box')
        .find('Input')
        .simulate('change', { target: { value: '' } });
    });
    wrapper.update();
    expect(wrapper.find('.option-item')).toHaveLength(options.length);
  });
  it('should render show Cascader Dropdown', () => {
    const op = options.map((item, index) => {
      const isLeaf = index % 2 === 0;
      return {
        ...item,
        isLeaf: index % 2 === 0,
        children: isLeaf
          ? []
          : [
              {
                ...item,
                label: `${item.label}-child`,
                value: `${item.value}-child`,
              },
            ],
      };
    });
    const onChangeFn = jest.fn();
    const onBlurFn = jest.fn();
    const wrapper = mount(
      <InputSelect className="input-select-inp" onChange={onChangeFn} onBlur={onBlurFn} value="dev" options={op} />,
    );
    act(() => {
      wrapper.find('Input.input-select-inp').prop('onFocus')(new Event('focus'));
    });
    wrapper.update();
    expect(wrapper.find('.input-select-dropdown-menu')).toExist();
    expect(wrapper.find('Cascader')).toExist();
    expect(wrapper.find('.option-item')).toHaveLength(op.length);
    expect(wrapper.find('.option-item').at(0).find('.arrow')).not.toExist();
    expect(wrapper.find('.option-item').at(1).find('.arrow')).toExist();
    act(() => {
      wrapper
        .find('.option-group-search')
        .find('Input')
        .simulate('change', { target: { value: options[0].value } });
    });
    wrapper.update();
    expect(wrapper.find('.option-item')).toHaveLength(1);
    act(() => {
      wrapper
        .find('.option-group-search')
        .find('Input')
        .simulate('change', { target: { value: '' } });
    });
    wrapper.update();
    act(() => {
      wrapper.find('.option-item').at(1).simulate('click');
    });
    wrapper.update();
    expect(wrapper.find('.option-item').last().html()).toContain(op[1].children[0].label);
    act(() => {
      wrapper.find('.option-item').last().simulate('click');
    });
    wrapper.update();
    // expect(onChangeFn).toHaveBeenLastCalledWith(op[1].children[0]);
  });
  it('should render with disabled', () => {
    const wrapper = mount(<InputSelect value="dev" disabled />);
    expect(wrapper.find('Input').prop('value')).toBe('dev');
    expect(wrapper.find('Input').prop('disabled')).toBe(true);
  });
});
