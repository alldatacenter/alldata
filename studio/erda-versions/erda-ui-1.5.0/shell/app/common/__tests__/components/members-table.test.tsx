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
import { MembersTable } from 'common';
import { mount, ReactWrapper } from 'enzyme';
import { MemberScope } from 'common/stores/member-scope';
import memberLabelStore from 'common/stores/member-label';
import userStore from 'app/user/stores';
import orgStore from 'app/org-home/stores/org';
import orgMemberStore from 'common/stores/org-member';
import appMemberStore from 'common/stores/application-member';
import { getDefaultPaging } from 'common/utils';
import { act } from 'react-dom/test-utils';
import _ from 'lodash';
import { message } from 'antd';

const memberLabels = [
  {
    label: 'Outsource',
    name: '外包人员',
  },
  {
    label: 'Partner',
    name: '合作伙伴',
  },
];

const currentOrg = {
  id: '27',
  creator: '12345t',
  desc: '',
  logo: '',
  name: 'cdp',
  displayName: 'dice-dev-pro',
  locale: '',
  isPublic: true,
  enableReleaseCrossCluster: false,
  selected: false,
  operation: '',
  status: '',
  type: 'FREE',
  publisherId: 0,
  domain: 'cdp-org.dev.terminus.io',
  version: 0,
  createdAt: '2021-06-21T09:43:02+08:00',
  updatedAt: '2021-06-21T09:43:02+08:00',
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

const list: IMember[] = [
  {
    id: '12345t',
    userId: '12345t',
    email: 'erda@erda.cloud',
    mobile: '123****5294',
    name: 'ERDA',
    nick: 'erda',
    avatar: '',
    status: '',
    scope: {
      type: 'org',
      id: '27',
    },
    roles: ['Lead'],
    labels: null,
    removed: false,
  },
  {
    id: '12345o',
    userId: '12345o',
    email: 'dice@erda.cloud',
    mobile: '124****5294',
    name: 'DICE',
    nick: 'dice',
    avatar: '',
    status: '',
    scope: {
      type: 'org',
      id: '27',
    },
    roles: ['Lead'],
    labels: null,
    removed: false,
  },
];

const roleMap = {
  Owner: 'Owner',
  Lead: 'Lead',
  PM: 'PM',
  PD: 'PD',
};

const $$ = <T extends Element = Element>(className: string): NodeListOf<T> => document.body.querySelectorAll(className);

const triggerTableAction = (wrapper: ReactWrapper, index: number, action: string) => {
  act(() => {
    const action1 = wrapper.find('.table-operations').at(index);
    const edit = action1.children().findWhere((n) => n.text() === action && n.hasClass('table-operations-btn'));
    edit.simulate('click');
  });
  wrapper.update();
};

const triggerDropdownSelect = (wrapper: ReactWrapper, key: string) => {
  act(() => {
    wrapper.find('DropdownSelect').prop('onClickMenu')({ key });
  });
  wrapper.update();
};

describe('MembersTable', () => {
  beforeAll(() => {
    jest.mock('common/stores/member-label');
    jest.mock('app/user/stores');
    jest.mock('app/org-home/stores/org');
    jest.mock('common/stores/org-member');
    jest.mock('common/stores/application-member');
    jest.mock('lodash');
    memberLabelStore.useStore = (fn) => fn({ memberLabels });
    orgStore.useStore = (fn) => fn({ currentOrg });
    userStore.useStore = (fn) => fn({ loginUser });
    orgMemberStore.useStore = (fn) => fn({ list, roleMap, paging: getDefaultPaging() });
    appMemberStore.effects.getRoleMap = jest.fn();
    _.debounce = (fn: Function) => fn;
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('MembersTable should work well', async () => {
    const spyError = jest.spyOn(message, 'error');
    jest.useFakeTimers();
    const getMemberLabels = jest.fn();
    const getRoleMap = jest.fn();
    const getMemberList = jest.fn();
    const cleanMembers = jest.fn();
    const updateMembers = jest.fn();
    const removeMember = jest.fn().mockResolvedValue();
    const genOrgInviteCode = jest.fn().mockResolvedValue({ verifyCode: 'verifyCode' });
    memberLabelStore.effects.getMemberLabels = getMemberLabels;
    orgMemberStore.effects.getRoleMap = getRoleMap;
    orgMemberStore.effects.updateMembers = updateMembers;
    orgMemberStore.effects.getMemberList = getMemberList;
    orgMemberStore.effects.removeMember = removeMember;
    orgMemberStore.effects.genOrgInviteCode = genOrgInviteCode;
    orgMemberStore.reducers.cleanMembers = cleanMembers;
    const wrapper = mount(<MembersTable scopeKey={MemberScope.ORG} />);
    expect(getMemberLabels).toHaveBeenLastCalledWith();
    expect(getRoleMap).toHaveBeenLastCalledWith({ scopeId: currentOrg.id, scopeType: 'org' });
    expect(wrapper.find('.table-operations')).toHaveLength(list.length);
    triggerDropdownSelect(wrapper, 'edit');
    expect(wrapper.find({ title: 'batch set the role of member' })).toExist();
    act(() => {
      wrapper
        .find('Table')
        .at(0)
        .prop('rowSelection')
        .onChange(list.map((t) => t.userId));
    });
    act(() => {
      wrapper.find({ title: 'batch set the role of member' }).at(0).prop('onOk')();
    });
    wrapper.update();
    expect(updateMembers).toHaveBeenLastCalledWith(
      {
        scope: {
          id: '27',
          type: 'org',
        },
        userIds: ['12345t', '12345o'],
      },
      { isSelf: true, queryParams: {} },
    );
    expect(wrapper.find({ title: 'batch set the role of member' })).not.toExist();
    triggerDropdownSelect(wrapper, 'authorize');
    expect(wrapper.find('BatchAuthorizeMemberModal').prop('visible')).toBeTruthy();
    act(() => {
      wrapper.find('BatchAuthorizeMemberModal').prop('onOk')({ applications: [1, 2], roles: ['PM', 'PD'] });
    });
    expect(updateMembers).toHaveBeenLastCalledWith({
      roles: ['PM', 'PD'],
      scope: { id: undefined, type: 'project' },
      targetScopeIds: [1, 2],
      targetScopeType: 'app',
      userIds: ['12345t', '12345o'],
    });
    triggerDropdownSelect(wrapper, 'remove');
    jest.runAllTimers();
    $$<HTMLButtonElement>('.ant-btn-primary')[2].click();
    expect(removeMember).toHaveBeenCalledTimes(1);
    act(() => {
      wrapper.find('FilterGroup').prop('onChange')({ query: 'erda-cloud', queryRole: 'admin', label: ['PM', 'PD'] });
    });
    triggerTableAction(wrapper, 0, 'edit');
    act(() => {
      wrapper.find({ title: 'set the role of member erda' }).at(0).prop('onOk')();
    });
    expect(updateMembers).toHaveBeenLastCalledWith(
      {
        scope: {
          id: '27',
          type: 'org',
        },
        userIds: ['12345t'],
      },
      {
        isSelf: true,
        queryParams: {
          label: ['PM', 'PD'],
          pageNo: 1,
          q: 'erda-cloud',
          roles: ['admin'],
        },
      },
    );
    triggerTableAction(wrapper, 0, 'exit');
    jest.runAllTimers();
    $$<HTMLButtonElement>('.ant-btn-primary')[2].click();
    expect(removeMember).toHaveBeenCalledTimes(2);
    act(() => {
      wrapper.find('.members-list').find('Button').at(0).simulate('click');
    });
    wrapper.update();
    expect(wrapper.find('AddMemberModal').prop('visible')).toBeTruthy();
    act(() => {
      wrapper.find('AddMemberModal').prop('toggleModal')();
    });
    wrapper.update();
    expect(wrapper.find('AddMemberModal').prop('visible')).toBeFalsy();
    await act(async () => {
      await wrapper.find('.members-list').find('Button').at(1).simulate('click');
    });
    wrapper.update();
    expect(genOrgInviteCode).toHaveBeenCalled();
    expect(wrapper.find('UrlInviteModal').prop('visible')).toBeTruthy();
    wrapper.find('UrlInviteModal').prop('onCancel')();
    wrapper.update();
    expect(wrapper.find('UrlInviteModal').prop('visible')).toBeFalsy();

    wrapper.unmount();
    expect(cleanMembers).toHaveBeenCalledTimes(1);
    spyError.mockReset();
  });
});
