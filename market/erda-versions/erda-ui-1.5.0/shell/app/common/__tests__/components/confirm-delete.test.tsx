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
import { ConfirmDelete } from 'common';
import { shallow, mount } from 'enzyme';
import i18n from 'i18n';

const deleteItem = 'project';
const confirmTip = 'confirm to delete this project';
const secondTitle = 'confirm to delete this project in secondTitle';
const title = 'this is a title';
const defaultConfirmTip = i18n.t('Permanently delete {deleteItem}. Please pay special attention to it.', {
  deleteItem,
});
const defaultSecondTitle = i18n.t('common:{deleteItem} cannot be restored after deletion. Continue?', {
  deleteItem,
});
const defaultTitle = i18n.t('common:confirm to delete current {deleteItem}', { deleteItem });

describe('ConfirmDelete', () => {
  it('render with default props ', () => {
    const wrapper = shallow(<ConfirmDelete deleteItem={deleteItem} />);
    expect(wrapper.find('.text-desc').text()).toBe(defaultConfirmTip);
    const temp = shallow(<div>{wrapper.find('Modal').prop('title')}</div>);
    expect(temp.html()).toContain(defaultTitle);
    expect(wrapper.find('p').text()).toBe(defaultSecondTitle);
  });
  it('render with customize props', () => {
    const onConfirmFn = jest.fn();
    const onCancelFn = jest.fn();
    const wrapper = mount(
      <ConfirmDelete
        onConfirm={onConfirmFn}
        onCancel={onCancelFn}
        deleteItem={deleteItem}
        confirmTip={confirmTip}
        secondTitle={secondTitle}
        title={title}
      >
        <div className="confirm-children" />
      </ConfirmDelete>,
    );
    expect(wrapper.find('.text-desc').text()).toBe(confirmTip);
    expect(wrapper.find('.confirm-children')).toExist();
    wrapper.find('span').at(0).simulate('click');
    expect(wrapper.find('p.mb-2').at(0).text()).toBe(secondTitle);
    expect(wrapper.find('.ant-modal-title').at(0).text()).toContain(title);
    expect(wrapper.find('Modal').prop('visible')).toBeTruthy();
    wrapper.find('.ant-modal-footer').children().at(1).simulate('click');
    expect(onConfirmFn).toHaveBeenCalled();
    expect(wrapper.find('Modal').prop('visible')).toBeFalsy();
    wrapper.find('span').at(0).simulate('click');
    wrapper.find('.ant-modal-footer').children().at(0).simulate('click');
    expect(onCancelFn).toHaveBeenCalled();
    expect(wrapper.find('Modal').prop('visible')).toBeFalsy();
  });
});
