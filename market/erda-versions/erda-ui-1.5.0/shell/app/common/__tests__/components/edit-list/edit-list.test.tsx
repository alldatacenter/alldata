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
import { EditList } from 'common';
import { validateValue } from 'common/components/edit-list/edit-list';
import { mount, shallow, ReactWrapper } from 'enzyme';
import { act } from 'react-dom/test-utils';

const dataTemp = [
  {
    title: 'name',
    name: 'name',
    key: 'name',
    render: {
      type: 'textarea',
      required: true,
      uniqueValue: true,
    },
  },
  {
    title: 'id',
    name: 'id',
    key: 'id',
    render: {
      required: false,
      uniqueValue: true,
    },
  },
  {
    title: 'gender',
    name: 'gender',
    key: 'gender',
    render: {
      type: 'select',
      required: true,
      uniqueValue: false,
      props: {
        options: [{ value: 'male' }, { value: 'female' }],
      },
    },
  },
  {
    title: 'work',
    name: 'work',
    key: 'work',
    render: {
      type: 'inputSelect',
      required: false,
      uniqueValue: false,
      rules: [
        {
          msg: '参数名为英文、数字、中划线或下划线',
          pattern: '/^[a-zA-Z0-9_-]*$/',
        },
      ],
    },
  },
];
const value = [
  {
    name: 'erda',
    id: '123',
    gender: 'male',
    work: 'coder',
  },
];

describe('edit-list', () => {
  describe('EditList', () => {
    const $$ = (wrapper: ReactWrapper<any, any>, index: number): ReactWrapper<any, any> => {
      return wrapper.find('.edit-list-item-box').at(index);
    };
    it('should render empty', () => {
      const changeFn = jest.fn();
      const blurSaveFn = jest.fn();
      const saveFn = jest.fn();
      const wrapper = shallow(<EditList onChange={changeFn} onBlurSave={blurSaveFn} onSave={saveFn} dataTemp={[]} />);
      expect(wrapper).toBeEmptyRender();
    });
    it('should work well', () => {
      jest.useFakeTimers();
      const changeFn = jest.fn();
      const blurSaveFn = jest.fn();
      const saveFn = jest.fn();
      const wrapper = mount(
        <EditList value={value} onChange={changeFn} onBlurSave={blurSaveFn} onSave={saveFn} dataTemp={dataTemp} />,
      );
      expect(wrapper.find('.edit-list-item-box')).toHaveLength(2);
      $$(wrapper, 1)
        .find('.edit-list-item-textarea')
        .simulate('change', { target: { value: '' } });
      expect($$(wrapper, 1).find('.edit-list-item').at(0)).toHaveClassName('has-error');
      $$(wrapper, 1).find('.edit-list-item-textarea').simulate('focus');
      $$(wrapper, 1)
        .find('.edit-list-item-textarea')
        .simulate('input', { target: { value: 'textarea' } });
      $$(wrapper, 1)
        .find('.edit-list-item-textarea')
        .simulate('change', { target: { value: 'textarea' } });
      $$(wrapper, 1).find('.edit-list-item-textarea').simulate('blur');
      jest.advanceTimersByTime(200);
      expect(blurSaveFn).toHaveBeenLastCalledWith([{ gender: 'male', id: '123', name: 'textarea', work: 'coder' }]);
      act(() => {
        $$(wrapper, 1).find('InputSelect').prop('onChange')('InputSelect');
        $$(wrapper, 1).find('InputSelect').prop('onBlur')();
      });
      jest.advanceTimersByTime(200);
      expect(blurSaveFn).toHaveBeenLastCalledWith([
        { gender: 'male', id: '123', name: 'textarea', work: 'InputSelect' },
      ]);
      $$(wrapper, 1)
        .find('input.nowrap')
        .simulate('change', { target: { value: '12234' } });
      $$(wrapper, 1).find('input.nowrap').simulate('blur');
      jest.advanceTimersByTime(200);
      expect(blurSaveFn).toHaveBeenLastCalledWith([
        { gender: 'male', id: '12234', name: 'textarea', work: 'InputSelect' },
      ]);
      wrapper.find('.edit-list-bottom').find('Button').simulate('click');
      expect(wrapper.find('.edit-list-item-box')).toHaveLength(3);
      wrapper.find('.table-operations-btn').at(1).simulate('click');
      expect(wrapper.find('.edit-list-item-box')).toHaveLength(2);
      wrapper.setProps({
        value: [...value, { id: 'id', gender: 'male', name: 'name', work: 'code' }],
        disabled: true,
      });
      wrapper.update();
      expect(wrapper.find('.edit-list-item-box')).toHaveLength(3);
    });
  });
  describe('validateValue', () => {
    it('should validateValue work well', () => {
      expect(validateValue(dataTemp, [{ name: 'erda' }, { name: 'erda' }])).toBe('name the same erda exists');
      expect(validateValue(dataTemp, [{ name: 'erda' }, { name: '' }])).toBe('name can not empty');
      expect(
        validateValue(dataTemp, [
          { name: 'erda', gender: 'male' },
          { name: 'erda.cloud', gender: 'male' },
        ]),
      ).toBe('');
    });
  });
});
