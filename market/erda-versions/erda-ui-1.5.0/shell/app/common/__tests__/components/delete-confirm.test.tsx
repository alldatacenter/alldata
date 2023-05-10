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
import { DeleteConfirm } from 'common';
import { shallow } from 'enzyme';
import i18n from 'i18n';

const defaultTitle = `${i18n.t('common:confirm deletion')}？`;
const defaultSecondTitle = `${i18n.t('common:confirm this action')}？`;

const $$ = (className: string) => {
  return document.body.querySelectorAll(className);
};

describe('DeleteConfirm', () => {
  it('render with warn', () => {
    let wrapper = shallow(<DeleteConfirm />);
    expect(wrapper).toEqual({});
    wrapper = shallow(
      <DeleteConfirm>
        <div className="children" />
        <div className="children" />
        <div className="children" />
      </DeleteConfirm>,
    );
    expect(wrapper.find('.children')).toHaveLength(3);
  });
  it('should render well', () => {
    const onShowFn = jest.fn();
    const onConfirmFn = jest.fn();
    const onCancelFn = jest.fn();
    jest.useFakeTimers();
    const wrapper = shallow(
      <DeleteConfirm countDown={3} onShow={onShowFn} onConfirm={onConfirmFn} onCancel={onCancelFn}>
        <div className="child">children</div>
      </DeleteConfirm>,
    );
    wrapper.find('.child').simulate('click', new Event('click'));
    jest.runAllTimers();
    expect(onShowFn).toHaveBeenCalled();
    expect(document.querySelector('.ant-modal-confirm-title')?.innerHTML).toContain(defaultTitle);
    expect($$('.ant-modal-confirm-content')[0].innerHTML).toBe(defaultSecondTitle);
    $$('.ant-btn')[0].click();
    expect(onCancelFn).toHaveBeenCalled();
    wrapper.find('.child').simulate('click', new Event('click'));
    jest.advanceTimersByTime(4000);
    $$('.ant-btn-primary')[0].click();
    expect(onConfirmFn).toHaveBeenCalled();
  });
});
