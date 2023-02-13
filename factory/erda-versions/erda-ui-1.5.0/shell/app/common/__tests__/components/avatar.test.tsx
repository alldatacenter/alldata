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
import { Avatar, AvatarList } from 'common';
import { shallow } from 'enzyme';
import userStore from 'user/stores';

const loginUser: ILoginUser = {
  id: '123456',
  name: 'dice',
  nick: 'dice-jest',
  avatar: '//terminus-paas.oss-cn-hangzhou.aliyuncs.com/uc/2017/08/04/f1d1edb4-a841-4b1b-bf68-3f5c6f6fcf17.jpeg',
  phone: '131098871132',
  email: 'dice@alibaba-inc.com',
  token: 'abc-123',
  orgId: 2,
  orgDisplayName: 'erda',
  orgName: 'erda',
  orgPublisherAuth: false,
  orgPublisherId: 12,
  isSysAdmin: false,
};

describe('Avatar', () => {
  userStore.reducers.setLoginUser(loginUser);
  const sizeResult = (size = 24) => ({ height: `${size}px`, width: `${size}px` });
  const url = 'i am img url';
  it('should support showName ', () => {
    const wrapper = shallow(<Avatar className="avatar-comp" showName name={loginUser.name} />);
    expect(wrapper.find('.avatar-comp')).toExist();
    expect(wrapper.find('Tooltip').find('span').text()).toBe(loginUser.name);
    expect(wrapper.find({ alt: 'user-avatar' })).not.toExist();
    wrapper.setProps({ showName: false });
    expect(wrapper.find('Tooltip')).not.toExist();
    expect(wrapper.find({ color: true }).prop('type')).toContain('head');
    wrapper.setProps({ showName: 'tooltip' });
    expect(wrapper.find('Tooltip')).toExist();
    expect(wrapper.find({ color: true }).prop('type')).toContain('head');
  });
  it('should support useLoginUser ', () => {
    const wrapper = shallow(<Avatar className="avatar-comp" showName name={loginUser.name} url={url} />);
    expect(wrapper.find('span').at(1).text()).toBe(loginUser.name);
    expect(wrapper.find({ alt: 'user-avatar' }).prop('src')).toContain(url);
    wrapper.setProps({ useLoginUser: true });
    expect(wrapper.find('span').at(1).text()).toBe(loginUser.nick);
    expect(wrapper.find({ alt: 'user-avatar' }).prop('src')).toContain(loginUser.avatar);
  });
  it('should support wrapClassName ', () => {
    const wrapper = shallow(
      <Avatar className="avatar-comp" showName name={loginUser.name} wrapClassName={'wrapClassName'} />,
    );
    expect(wrapper.find('.wrapClassName')).toExist();
  });
  it('should support size ', () => {
    const wrapper = shallow(<Avatar className="avatar-comp" showName name={loginUser.name} />);
    expect(wrapper.find('.dice-avatar').prop('style')).toStrictEqual(sizeResult(24));
    wrapper.setProps({ size: 100 });
    expect(wrapper.find('.dice-avatar').prop('style')).toStrictEqual(sizeResult(100));
  });
});
