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
import { FormModal } from 'common';
import { mount } from 'enzyme';
import { act } from 'react-dom/test-utils';

const fieldsList = [
  {
    name: 'id',
    itemProps: {
      title: 'hidden-id',
      type: 'hidden',
    },
  },
  {
    label: 'apiName',
    name: 'apiName',
    type: 'input',
    required: true,
    itemProps: {
      className: 'api-name-field',
    },
  },
  {
    label: 'startTime',
    name: 'startTime',
    type: 'datePicker',
    required: false,
    itemProps: {
      className: 'start-time-field',
    },
  },
];

const formData = {
  id: '12',
  apiName: 'erda-api',
  startTime: '2021-07-20',
};

const PureForm = () => {
  return <div className="custom-form">PureForm</div>;
};

describe('FormModal', () => {
  afterEach(() => {
    jest.clearAllTimers();
  });
  it('FormModal should work well with hidden field', async () => {
    jest.useFakeTimers();
    const cancelFn = jest.fn();
    const okFn = jest.fn().mockResolvedValue(formData);
    const wrapper = mount(
      <FormModal title="project" visible={false} fieldsList={fieldsList} onCancel={cancelFn} onOk={okFn} />,
    );
    wrapper.setProps({
      width: 1200,
      formData: {
        apiName: 'erda-api',
        startTime: '2021-07-20',
      },
      visible: true,
    });
    jest.runAllTimers();
    const submitBtn = wrapper.find('Button').findWhere((t) => t.key() === 'submit');
    await act(async () => {
      await submitBtn.simulate('click');
    });
    expect(okFn).toHaveBeenCalled();
  });
  it('FormModal should work well', async () => {
    jest.useFakeTimers();
    const cancelFn = jest.fn();
    const okFn = jest.fn().mockResolvedValue(formData);
    const beforeSubmitFn = jest.fn().mockResolvedValue(formData);
    const wrapper = mount(
      <FormModal
        title="project"
        visible={false}
        fieldsList={fieldsList}
        onCancel={cancelFn}
        beforeSubmit={beforeSubmitFn}
        onOk={okFn}
      />,
    );
    expect(wrapper.find('Modal').prop('width')).toBe(600);
    expect(wrapper.find('Modal').prop('title')).toBe('project');
    wrapper.setProps({
      title: undefined,
      name: 'app',
      visible: true,
      formData: {},
    });
    const cancelBtn = wrapper.find('Button').findWhere((t) => t.key() === 'back');
    const submitBtn = wrapper.find('Button').findWhere((t) => t.key() === 'submit');
    expect(wrapper.find('Modal').prop('title')).toBe('add app');
    expect(wrapper.find('Button')).toHaveLength(2);
    act(() => {
      cancelBtn.simulate('click');
    });
    jest.runAllTimers();
    expect(cancelFn).toHaveBeenCalled();
    wrapper.setProps({
      width: 1200,
      formData,
      visible: true,
    });
    jest.runAllTimers();
    expect(wrapper.find('Modal').prop('title')).toBe('edit app');
    expect(wrapper.find({ title: 'apiName' })).toHaveClassName('ant-form-item-required');
    expect(wrapper.find({ title: 'startTime' })).not.toHaveClassName('ant-form-item-required');
    expect(wrapper.find('Modal').prop('width')).toBe(1200);
    await act(async () => {
      await submitBtn.simulate('click');
    });
    expect(beforeSubmitFn).toHaveBeenCalled();
    const { startTime, apiName, id } = beforeSubmitFn.mock.calls[0][0];
    expect({ startTime: startTime?.format('YYYY-MM-DD') ?? '', apiName, id }).toStrictEqual(formData);
    expect(okFn).toHaveBeenCalled();
  });
  it('FormModal render without fieldsList', async () => {
    jest.useFakeTimers();
    const cancelFn = jest.fn();
    const okFnPromise = jest.fn().mockRejectedValue(formData);
    const okFn = jest.fn();
    const wrapper = mount(
      <FormModal title="project" visible={true} onCancel={cancelFn} onOk={okFnPromise} PureForm={PureForm} />,
    );
    const submitBtn = wrapper.find('Button').findWhere((t) => t.key() === 'submit');
    wrapper.setProps({
      width: 1200,
      formData,
    });
    jest.runTimersToTime(2000);
    expect(wrapper.find('.custom-form')).toExist();
    await act(async () => {
      await submitBtn.simulate('click');
    });
    expect(okFnPromise).toHaveBeenCalled();
    wrapper.setProps({
      onOk: okFn,
    });
    await act(async () => {
      await submitBtn.simulate('click');
    });
    expect(okFn).toHaveBeenCalled();
  });
});
