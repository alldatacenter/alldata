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
import { CRUDTable } from 'common';
import { Input } from 'antd';
import { mount, shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';
import { createStore } from 'core/cube';

const columns = [
  {
    title: 'NAME',
    dataIndex: 'name',
  },
];
const filterConfig = [
  {
    type: Input,
    name: 'name',
    initialValue: 'erda',
  },
  {
    type: Input,
    name: 'org',
  },
];

describe('crud-table.', () => {
  let edit: any;
  const getColumns = ({ onEdit }) => {
    edit = onEdit;
    return [...columns];
  };
  const getFieldsList = () => {
    return [
      {
        name: 'formName',
        label: 'formName',
      },
    ];
  };
  describe('CRUDTable', () => {
    it('should work well', () => {
      const extraOperation = <div className="extraOperation">extraOperation</div>;
      const clearListFn = jest.fn();
      const submitFn = jest.fn();
      const asyncSubmitFn = jest.fn().mockResolvedValue();
      const onModalCloseFn = jest.fn();
      const wrapper = mount(
        <CRUDTable
          filterConfig={filterConfig}
          getColumns={getColumns}
          clearList={clearListFn}
          showTopAdd
          extraOperation={extraOperation}
          addAuthTooltipTitle="noAuth"
          handleFormSubmit={submitFn}
          onModalClose={onModalCloseFn}
        />,
      );
      expect(wrapper.find('.extraOperation')).toExist();
      wrapper.setProps({
        extraOperation: () => <div className="extraOperation-fn">extraOperation</div>,
        getFieldsList,
      });
      expect(wrapper.find('.extraOperation-fn')).toExist();
      expect(wrapper.find('FormModalComp')).toExist();
      wrapper.find({ noAuthTip: 'noAuth' }).find('Button').simulate('click');
      expect(wrapper.find('FormModalComp').prop('visible')).toBeTruthy();
      act(() => {
        wrapper.find('FormModalComp').prop('onOk')({ formName: 1 });
      });
      expect(submitFn).toHaveBeenLastCalledWith({ formName: 1 }, false);
      expect(onModalCloseFn).toHaveBeenLastCalledWith(false);
      wrapper.setProps({
        handleFormSubmit: asyncSubmitFn,
      });
      act(() => {
        edit({ formName: 'erda' });
        wrapper.find('FormModalComp').prop('onOk')({ formName: 2 });
      });
      expect(asyncSubmitFn).toHaveBeenLastCalledWith({ formName: 2 }, false);
      wrapper.unmount();
      expect(clearListFn).toHaveBeenCalled();
    });
  });
  describe('CRUDTable.StoreTable', () => {
    it('should work well', () => {
      const getList = jest.fn();
      const addItem = jest.fn();
      const updateItem = jest.fn();
      const submitFn = jest.fn();
      const list = [{ name: 'erda' }];
      const paging = { pageNo: 1 };
      const data = { name: 'erda' };
      const store = createStore({
        name: 'CRUDTable.StoreTable',
        state: {
          list,
          paging,
        },
      });
      const effects = {
        getList,
        addItem,
        updateItem,
      };
      const wrapper = shallow(<CRUDTable.StoreTable store={{ ...store, effects }} getColumns={getColumns} />);
      expect(wrapper.find('CRUDTable').prop('list')).toStrictEqual(list);
      expect(wrapper.find('CRUDTable').prop('paging')).toStrictEqual(paging);
      const handleFormSubmit = wrapper.find('CRUDTable').prop('handleFormSubmit');
      handleFormSubmit(data, false);
      expect(addItem).toHaveBeenLastCalledWith(data);
      handleFormSubmit(data, true);
      expect(updateItem).toHaveBeenLastCalledWith(data);
      wrapper.setProps({
        handleFormSubmit: submitFn,
      });
      wrapper.find('CRUDTable').prop('handleFormSubmit')(data, true);
      expect(submitFn).toHaveBeenCalled();
    });
  });
});
