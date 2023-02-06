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
import { DetailsPanel } from 'common';
import { mount } from 'enzyme';

const baseInfoConf = {
  title: 'DetailsPanel',
  panelProps: {
    fields: [
      {
        label: 'domain',
        value: 'https://www.erda.cloud',
      },
      {
        label: 'name',
        value: 'erda',
      },
    ],
  },
};

const props = {
  baseInfoConf,
  linkList: [
    {
      key: 'API',
      showTitle: true,
      linkProps: {
        title: 'API',
        icon: <div className="api-icon" />,
      },
      panelProps: {
        fields: [],
      },
    },
    {
      key: 'TEST',
      showTitle: false,
      linkProps: {
        title: 'TEST',
        icon: <div className="test-icon" />,
      },
      getComp: () => <div className="get-comp">getComp</div>,
    },
  ],
};

describe('DetailsPanel', () => {
  it('should render well', () => {
    const wrapper = mount(<DetailsPanel {...props} />);
    expect(wrapper.find({ title: 'DetailsPanel' })).toExist();
    expect(wrapper.find('AnchorLink')).toHaveLength(2);
    expect(wrapper.find('.get-comp')).toExist();
    expect(wrapper.find('.api-icon')).toExist();
  });
});
