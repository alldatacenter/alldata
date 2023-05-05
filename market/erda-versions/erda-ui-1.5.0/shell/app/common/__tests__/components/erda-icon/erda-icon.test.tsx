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
import { mount } from 'enzyme';
import { ErdaIcon } from 'common';

describe('erda-icon', () => {
  describe('ErdaIcon', () => {
    it('render with iconType', () => {
      const fn = jest.fn();
      const wrapper = mount(<ErdaIcon type="lock" isConfigPageIcon className="customize-cls" onClick={fn} />);
      expect(wrapper.find('.i-icon').hasClass('customize-cls')).toBeTruthy();
      wrapper.find('.i-icon').simulate('click');
      expect(fn).toHaveBeenCalled();
    });
    it('render with illegal icon Type', () => {
      const wrapper = mount(<ErdaIcon type="illegal" />);
      expect(wrapper).toHaveHTML('<span>Not Exists</span>');
    });
  });
});
