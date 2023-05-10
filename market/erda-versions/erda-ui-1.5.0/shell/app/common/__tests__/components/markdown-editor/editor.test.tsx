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
import { Editor } from 'common/components/markdown-editor/editor';
import { shallow } from 'enzyme';
import agent from 'agent';
import * as common from 'common/utils';

describe('Editor', () => {
  beforeAll(() => {
    jest.mock('agent');
    jest.spyOn(common, 'convertToFormData').mockImplementation(() => {});
  });
  it('should Editor work fine', () => {
    const file = new File([], 'xxx.png', { type: 'image/png' });
    const itemsInfo = [{ type: 'image/png', kind: 'string' }];
    const wrapper = shallow(<Editor value={'### erda cloud'} />);
    agent.post = () => ({
      send: jest.fn().mockResolvedValue({
        body: {
          success: true,
          data: {
            url: 'img/path',
          },
        },
      }),
    });
    expect(wrapper.prop('renderHTML')('### erda cloud')).toContain('<h3 id="erda-cloud">erda cloud</h3>');
    expect(wrapper.prop('onImageUpload')(file, '//{url}', itemsInfo)).resolves.toBe('//img/path');
  });
  afterAll(() => {
    jest.clearAllMocks();
  });
});
