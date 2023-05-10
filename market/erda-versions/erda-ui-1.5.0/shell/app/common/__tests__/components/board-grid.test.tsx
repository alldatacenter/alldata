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
import { BoardGrid } from 'common';
import { shallow } from 'enzyme';

const props = {
  name: 'board-grid',
  id: 'board-grid',
};
describe('board-grid', () => {
  describe('BoardGrid', () => {
    it('should render well', () => {
      const wrapper = shallow(<BoardGrid {...props} />);
      expect(wrapper.prop('name')).toBe(props.name);
      expect(wrapper.prop('id')).toBe(props.id);
    });
  });
  describe('BoardGrid.Pure', () => {
    const url = '/api/board/query';
    const layout = {
      issue: {
        view: {
          api: {
            url,
          },
        },
      },
      story: {
        view: {},
      },
    };
    it('should render well', () => {
      const wrapper = shallow(<BoardGrid.Pure {...props} layout={layout} />);
      expect(wrapper.prop('name')).toBe(props.name);
      expect(wrapper.prop('id')).toBe(props.id);
    });
  });
});
