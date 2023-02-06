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
import { ErrorBoundary } from 'common';
import { createBrowserHistory } from 'history';
import { getConfig, setConfig } from 'core/config';

const Something = () => null;

describe('ErrorBoundary', () => {
  beforeAll(() => {
    const browserHistory = createBrowserHistory();
    setConfig('history', browserHistory);
  });
  afterAll(() => {
    setConfig('history', undefined);
  });
  it('should render if wrapped component throws', () => {
    const wrapper = mount(
      <ErrorBoundary>
        <Something />
      </ErrorBoundary>,
    );
    const error = new Error('test');
    wrapper.find(Something).simulateError(error);
    wrapper.find('Button').simulate('click');
    const history = getConfig('history');

    expect(history.location.pathname).toBe('/');
  });
});
