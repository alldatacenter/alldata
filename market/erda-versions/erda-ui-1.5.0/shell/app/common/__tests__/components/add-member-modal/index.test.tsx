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
import { AddMemberModal } from 'common';
import { mount, shallow } from 'enzyme';
import { MemberScope } from 'common/stores/member-scope';
import projectMemberStore from 'common/stores/project-member';
import appMemberStore from 'common/stores/application-member';
import * as Services from 'common/services';

const scope = {
  id: '123',
  type: MemberScope.PROJECT,
};

const roleMap = {
  Owner: 'Owner',
  Lead: 'Lead',
  PM: 'PM',
  PD: 'PD',
};

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

const queryParams = {
  org: 'erda',
};

const data = {
  total: 100,
  list: [
    {
      id: 1,
      name: 'erda-ui',
      userId: '1',
      key: '1',
      memberRoles: 'DEV',
    },
  ],
};

describe('AddMemberModal', () => {
  beforeAll(() => {
    jest.mock('common/stores/project-member');
    jest.mock('common/stores/application-member');
    jest.mock('common/services');
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('AddMemberModal should work well', async () => {
    const toggleModalFn = jest.fn();
    const addMembers = jest.fn().mockResolvedValue();
    const updateMembers = jest.fn();
    const getRoleMap = jest.fn();
    const getApps = jest.fn().mockResolvedValue({
      success: true,
      data,
    });
    projectMemberStore.effects.addMembers = addMembers;
    projectMemberStore.effects.updateMembers = updateMembers;
    appMemberStore.effects.getRoleMap = getRoleMap;
    Object.defineProperty(Services, 'getApps', {
      value: getApps,
    });
    const wrapper = mount(
      <AddMemberModal
        hasConfigAppAuth
        visible={false}
        queryParams={queryParams}
        scope={scope}
        roleMap={roleMap}
        memberLabels={memberLabels}
        toggleModal={toggleModalFn}
      />,
    );
    expect(getRoleMap).toHaveBeenLastCalledWith({ scopeType: MemberScope.APP });
    const modal = wrapper.find({ title: 'add member' });
    expect(modal.at(0).prop('beforeSubmit')({})).toStrictEqual({});
    expect(modal.at(0).prop('beforeSubmit')({ app_roles: ['PM', 'PD'] })).toBeNull();
    expect(modal.at(0).prop('beforeSubmit')({ applications: ['1'] })).toBeNull();
    expect(modal.at(0).prop('beforeSubmit')({ applications: ['1'], app_roles: ['PM', 'PD'] })).toStrictEqual({
      applications: ['1'],
      app_roles: ['PM', 'PD'],
    });
    await modal.at(0).prop('onOk')({ applications: ['1'], app_roles: ['PM', 'PD'], userIds: 1 });
    expect(addMembers).toHaveBeenLastCalledWith({ scope, userIds: 1 }, { queryParams: { org: 'erda' } });
    expect(updateMembers).toHaveBeenLastCalledWith(
      { roles: ['PM', 'PD'], scope, targetScopeIds: ['1'], targetScopeType: 'app', userIds: 1 },
      { successMsg: false },
    );
    modal.at(0).prop('onCancel')();
    expect(toggleModalFn).toHaveBeenCalled();
    const fieldsList = modal.at(0).prop('fieldsList');
    expect(fieldsList).toHaveLength(5);
    const fieldWrapper = shallow(
      <div key="filed-wrapper">
        {fieldsList.map((item) => {
          const comp = item.getComp ? item.getComp() : null;
          return <div key={item.name}>{comp}</div>;
        })}
      </div>,
    );
    expect(fieldWrapper.find('MemberSelector.Add').prop('scopeType')).toBe(MemberScope.PROJECT);
    await fieldWrapper.find('LoadMoreSelector').prop('getData')();
    expect(getApps).toHaveBeenCalled();
    expect(fieldWrapper.find('LoadMoreSelector').prop('dataFormatter')(data)).toStrictEqual({
      ...data,
      list: data.list.map((item) => ({
        ...item,
        label: item.name,
        value: item.id,
      })),
    });
  });
});
