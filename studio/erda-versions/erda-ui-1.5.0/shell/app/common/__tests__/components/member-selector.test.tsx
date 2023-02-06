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
import { MemberSelector } from 'common';
import { UserSelector, chosenItemConvert, getNotFoundContent } from 'common/components/member-selector';
import { mount, shallow } from 'enzyme';
import * as Services from 'common/services';
import routeInfoStore from 'core/stores/route';
import userStore from 'app/user/stores';
import orgMemberStore from 'common/stores/org-member';
import { act } from 'react-dom/test-utils';
import { MemberScope } from 'common/stores/member-scope';
import { sleep } from '../../../../test/utils';
import _ from 'lodash';

const routerData = {
  params: {
    projectId: 11,
    appId: 22,
  },
};

const loginUser = {
  id: '12345t',
  email: '',
  nick: 'erda',
  name: 'ERDA',
  phone: '',
  avatar: '',
  token: 'abc',
  isSysAdmin: false,
};

const data = {
  total: 100,
  list: [
    {
      id: '12345t',
      email: '',
      nick: 'erda',
      name: 'ERDA',
      phone: '',
      avatar: '',
      token: 'abc',
      isSysAdmin: false,
      userId: '12345t',
      roles: ['DEV', 'Owner'],
    },
    {
      id: '12345d',
      email: '',
      name: 'Dice',
      phone: '',
      avatar: '',
      token: 'abc',
      isSysAdmin: false,
      userId: '12345d',
      roles: ['DEV', 'Lead', 'Report'],
    },
  ],
};

const roleMap = {
  Owner: 'Owner',
  Lead: 'Lead',
  DEV: 'DEV',
  PM: 'PM',
  PD: 'PD',
};

const scopeId = '2';
const scopeType = 'org';

const categorys = [{ label: 'DEV', value: 'dev' }];

describe('member-selector', () => {
  beforeAll(() => {
    jest.mock('core/stores/route');
    jest.mock('app/user/stores');
    jest.mock('common/services');
    jest.mock('common/stores/org-member');
    jest.mock('lodash');
    routeInfoStore.useStore = (fn) => fn(routerData);
    userStore.useStore = (fn) => fn({ loginUser });
    userStore.getState = (fn) => fn({ loginUser });
    orgMemberStore.useStore = (fn) => fn({ roleMap });
    _.debounce = (fn: Function) => fn;
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('MemberSelector render with uc', () => {
    const wrapper = shallow(<MemberSelector scopeType="uc" />);
    expect(wrapper.find('UserSelector')).toExist();
  });
  it('MemberSelector should work well', () => {
    const getUsers = jest.fn().mockResolvedValue({
      success: true,
      data,
    });
    const getMembers = jest.fn().mockResolvedValue({
      success: true,
      data,
    });
    const onChange = jest.fn();
    const getRoleMap = jest.fn();
    const deleteValueFn = jest.fn();
    orgMemberStore.effects.getRoleMap = getRoleMap;
    orgMemberStore.effects.getRoleMap = getRoleMap;
    Object.defineProperty(Services, 'getUsers', {
      value: getUsers,
    });
    Object.defineProperty(Services, 'getMembers', {
      value: getMembers,
    });
    const wrapper = mount(
      <MemberSelector scopeType={scopeType} scopeId={scopeId} type="Category" onChange={onChange} />,
    );
    expect(getRoleMap).toHaveBeenLastCalledWith({ scopeType: 'org', scopeId: '2' });
    expect(wrapper.find('LoadMoreSelector').prop('dataFormatter')(data)).toStrictEqual({
      ...data,
      list: data.list.map(({ name, userId, nick, ..._rest }) => ({
        ..._rest,
        userId,
        nick,
        name,
        label: nick || name,
        value: userId,
      })),
    });
    wrapper.setProps({
      isStaticCategory: true,
      categorys,
    });
    wrapper.update();
    expect(wrapper.find('LoadMoreSelector').prop('category')).toStrictEqual(categorys);

    act(() => {
      wrapper.find('LoadMoreSelector').prop('getData')({ category: 'DEV', name: 'erda' });
    });

    expect(getMembers).toHaveBeenLastCalledWith({
      name: 'erda',
      roles: ['DEV'],
      scopeId,
      scopeType,
    });

    const optionRenderWrapper = shallow(
      <div>{data.list.map((item) => wrapper.find('LoadMoreSelector').prop('optionRender')(item, 'normal'))}</div>,
    );
    expect(optionRenderWrapper.find({ title: 'ERDA' }).text()).toContain('erda');
    expect(optionRenderWrapper.find({ title: 'Dice' }).text()).toContain('none');
    const valueItemRenderWrapper = shallow(
      <div className="value-item-wrapper">
        {data.list.map((item, index) =>
          wrapper.find('LoadMoreSelector').prop('valueItemRender')(item, deleteValueFn, index % 2 === 0),
        )}
      </div>,
    );
    expect(valueItemRenderWrapper.find('.value-item-wrapper').childAt(0).name()).toBe('WrappedTag');
    valueItemRenderWrapper.find('WrappedTag').prop('onClose')();
    expect(deleteValueFn).toHaveBeenLastCalledWith(data.list[0]);
    expect(wrapper.find('LoadMoreSelector').prop('quickSelect')).toStrictEqual([]);
    wrapper.setProps({
      selectSelfInOption: true,
    });
    const quickSelect = shallow(<div>{wrapper.find('LoadMoreSelector').prop('quickSelect')}</div>);
    quickSelect.find('a').simulate('click');
    expect(onChange).toHaveBeenCalled();
    wrapper.setProps({
      showSelfChosen: true,
    });
    wrapper.find('a').simulate('click');
    expect(onChange).toHaveBeenCalledTimes(2);
    expect(onChange).toHaveBeenCalled();
  });
  it('MemberSelector.Add should work well', () => {
    const orgWrapper = shallow(<MemberSelector.Add scopeType={MemberScope.ORG} />);
    expect(orgWrapper.find('UserSelector')).toExist();
    const otherWrapper = shallow(<MemberSelector.Add scopeType={MemberScope.PROJECT} />);
    expect(otherWrapper.find('UserSelector')).not.toExist();
  });
  it('UserSelector should work well', async () => {
    const getUsers = jest.fn().mockResolvedValue({
      success: true,
      data: {
        users: data.list,
      },
    });
    Object.defineProperty(Services, 'getUsers', {
      value: getUsers,
    });
    Object.defineProperty(Services, 'getUsersNew', {
      value: getUsers,
    });

    const wrapper = mount(<UserSelector value="12345t" />);
    await sleep(1500);
    expect(getUsers).toHaveBeenLastCalledWith({ userID: ['12345t'] });
    await act(async () => {
      await wrapper.find('Select').prop('onSearch')('erda');
    });
    expect(getUsers).toHaveBeenLastCalledWith({ pageNo: 1, pageSize: 15, q: 'erda' });
  });
  it('chosenItemConvert should work well', () => {
    const exitUser = {
      'erda-2021': {
        name: 'erda',
        id: 'erda-2021',
      },
      'dice-2021': {
        name: 'erda',
        id: 'dice-2021',
      },
    };
    const values = { value: 'erda-2021' };
    expect(chosenItemConvert(values, exitUser)).toStrictEqual([
      {
        ...exitUser['erda-2021'],
        ...values,
        label: 'erda',
      },
    ]);
  });
  it('getNotFoundContent should work well', () => {
    expect(getNotFoundContent('uc')).toBeUndefined();
    expect(getNotFoundContent(MemberScope.PROJECT)).toBe('please confirm that the user has joined the project');
    expect(getNotFoundContent(MemberScope.APP)).toBe('please confirm that the user has joined the application');
  });
});
