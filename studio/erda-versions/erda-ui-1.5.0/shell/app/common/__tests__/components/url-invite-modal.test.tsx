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
import { UrlInviteModal } from 'common/components/members-table/url-invite-modal';
import { mount } from 'enzyme';

describe('UrlInviteModal', () => {
  it('should ', () => {
    const onCancelFn = jest.fn();
    const url = 'https:www.erda.cloud';
    const code = 'erda';
    const wrapper = mount(<UrlInviteModal visible onCancel={onCancelFn} code={code} tip="tips" url={url} />);
    expect(wrapper.find('Alert')).toExist();
    expect(wrapper.find('Alert').prop('message')).toBe('tips');
    expect(wrapper.find('Alert').prop('message')).toBe('tips');
    expect(wrapper.find('Input').at(0).prop('value')).toBe(url);
    expect(wrapper.find('Input').at(1).prop('value')).toBe(code);
    expect(wrapper.find('span.cursor-copy').prop('data-clipboard-tip')).toBe('invitation link and verification code');
    wrapper.setProps({
      code: undefined,
      tip: undefined,
      url: undefined,
      modalProps: {
        className: 'UrlInviteModal',
      },
    });
    expect(wrapper.find('Modal')).toHaveClassName('UrlInviteModal');
    expect(wrapper.find('Alert')).not.toExist();
    expect(wrapper.find('Input')).toHaveLength(1);
    expect(wrapper.find('span.cursor-copy').prop('data-clipboard-tip')).toBe('invitation link');
  });
});
