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
import AuthorizeMemberModal from 'common/components/authorize-member-modal';
import appMemberStore from 'common/stores/application-member';
import orgMemberStore from 'common/stores/org-member';
import { mount, shallow } from 'enzyme';
import * as Services from 'common/services';
import { sleep } from '../../../../../test/utils';
import routeInfoStore from 'core/stores/route';
import { act } from 'react-dom/test-utils';

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

const roleMap = {
  dev: 'DEV',
  pm: 'PM',
};

describe('AuthorizeMemberModal', () => {
  beforeAll(() => {
    jest.mock('common/stores/org-member');
    jest.mock('common/stores/application-member');
    jest.mock('common/services');
    jest.mock('core/stores/route');
    routeInfoStore.getState = (fn) => {
      return fn({
        params: {
          projectId: 1,
        },
      });
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('AuthorizeMemberModal work well ', async () => {
    const closeModalFn = jest.fn();
    const updateMembersFn = jest.fn().mockResolvedValue();
    const removeMemberFn = jest.fn().mockResolvedValue();
    const getRoleMapFn = jest.fn();
    const getApps = jest.fn().mockResolvedValue({
      success: true,
      data,
    });
    Object.defineProperty(Services, 'getAppList', {
      value: getApps,
    });
    appMemberStore.useStore = (fn) => {
      return fn({ roleMap });
    };
    appMemberStore.effects.getRoleMap = getRoleMapFn;
    orgMemberStore.effects.updateMembers = updateMembersFn;
    orgMemberStore.effects.removeMember = removeMemberFn;
    const wrapper = mount(
      <AuthorizeMemberModal type={'org'} closeModal={closeModalFn} member={{ userId: 'erda-1' }} />,
    );
    await sleep(1500);
    expect(getRoleMapFn).toHaveBeenLastCalledWith({ scopeType: 'app' });
    expect(getApps).toHaveBeenLastCalledWith({ memberID: 'erda-1', pageNo: 1, pageSize: 15, projectId: 1 });
    expect(wrapper.find('Modal').prop('visible')).toBeTruthy();
    const { render } = wrapper.find('Table').at(0).prop('columns')[1];
    const select = shallow(<div>{render('erda', data.list[0])}</div>);
    act(() => {
      select.find({ mode: 'multiple' }).prop('onChange')([]);
    });
    expect(removeMemberFn).toHaveBeenLastCalledWith({
      scope: {
        id: '1',
        type: 'app',
      },
      userIds: ['erda-1'],
    });
    expect(getApps).toHaveBeenLastCalledWith({ memberID: 'erda-1', pageNo: 1, pageSize: 15, projectId: 1 });
    act(() => {
      select.find({ mode: 'multiple' }).prop('onChange')(['pm']);
    });
    expect(updateMembersFn).toHaveBeenLastCalledWith(
      {
        roles: ['pm'],
        scope: {
          id: '1',
          type: 'app',
        },
        userIds: ['erda-1'],
      },
      { forbidReload: true },
    );
    expect(getApps).toHaveBeenLastCalledWith({ memberID: 'erda-1', pageNo: 1, pageSize: 15, projectId: 1 });
    wrapper.unmount();
  });
});
