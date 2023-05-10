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
import { PagingTable } from 'common';
import { shallow } from 'enzyme';

const dataSource = [{ name: 'erda' }];
const columns = [
  {
    title: 'NAME',
    dataIndex: 'name',
  },
];

describe('PagingTable', () => {
  it('should render well', () => {
    jest.useFakeTimers();
    const getList = jest.fn();
    const addFn = jest.fn();
    const clearListFn = jest.fn();
    const editFn = jest.fn();
    const deleteFn = jest.fn();
    const wrapper = shallow(
      <PagingTable
        isForbidInitialFetch
        getList={getList}
        buttonClass="add-button"
        onAdd={addFn}
        clearList={clearListFn}
        columns={columns}
        onEdit={editFn}
        onDelete={deleteFn}
      />,
    );
    expect(getList).toHaveBeenCalledTimes(0);
    wrapper.find('.add-button').simulate('click');
    expect(addFn).toHaveBeenCalled();
    wrapper.setProps({
      title: 'project list',
      dataSource,
    });
    expect(wrapper.find('.table-title').text()).toBe('project list');
    expect(wrapper.find('WrappedTable').prop('columns')).toHaveLength(columns.length);
    wrapper.setProps({
      basicOperation: true,
    });
    expect(wrapper.find('WrappedTable').prop('columns')).toHaveLength(columns.length + 1);
    const wrapperInstance = wrapper.instance();
    wrapperInstance.onChangePage(2);
    expect(wrapperInstance.page).toStrictEqual({ pageNo: 2, pageSize: 15 });
    expect(getList).toHaveBeenLastCalledWith({ pageNo: 2, pageSize: 15 });
    const temp = shallow(<div>{wrapperInstance.operation.render(dataSource[0])}</div>);
    temp.find('.table-operations-btn').at(0).simulate('click');
    expect(editFn).toHaveBeenLastCalledWith(dataSource[0]);
    // show confirm modal
    temp.find('.table-operations-btn').at(1).simulate('click');
    // trigger delete event
    jest.runAllTimers();
    document.querySelectorAll('.ant-btn-primary')[0].click();
    expect(deleteFn).toHaveBeenLastCalledWith(dataSource[0]);
    wrapper.unmount();
    expect(clearListFn).toHaveBeenCalled();
  });
  it('should init fetch', () => {
    const getList = jest.fn();
    shallow(<PagingTable isForbidInitialFetch={false} getList={getList} />);
    expect(getList).toHaveBeenCalledTimes(1);
  });
});
