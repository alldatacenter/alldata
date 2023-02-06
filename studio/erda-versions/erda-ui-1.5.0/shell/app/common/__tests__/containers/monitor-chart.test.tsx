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
import { MonitorChart } from 'common';
import { render } from 'react-dom';
import agent from 'agent';

let container: HTMLDivElement;

describe('MonitorChart', () => {
  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    document.body.removeChild(container);
    container = null;
  });
  beforeAll(() => {
    jest.mock('agent');
    agent.get = (r) => {
      return {
        query: jest.fn().mockResolvedValue({
          body: {
            success: true,
            data: {
              url: 'img/path',
            },
          },
        }),
      };
    };
  });
  it('should ', (done) => {
    render(
      <MonitorChart
        resourceType="erda"
        resourceId="01"
        metricKey="a"
        chartQuery={{
          fetchMetricKey: 'cloud',
        }}
      />,
      container,
    );
    done();
  });
});
