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
import AddonSettings from 'common/components/addon-settings';
import routeInfoStore from 'core/stores/route';
import { mount } from 'enzyme';
import agent from 'agent';
import { act } from 'react-dom/test-utils';

const { PureAddonSettings } = AddonSettings;

const insId = 123;
const routerData = {
  params: {
    insId,
  },
};
const data = {
  name: 'erda',
};

jest.mock('core/stores/route');
jest.mock('agent');

describe('addon-settings', () => {
  const fn = jest.fn();
  beforeAll(() => {
    routeInfoStore.getState = (fn) => {
      return fn(routerData);
    };
    routeInfoStore.useStore = (fn) => {
      return fn(routerData);
    };
    agent.get = jest.fn().mockResolvedValue({
      body: {
        success: true,
        data,
      },
    });
    agent.delete = () => {
      fn.call(null);
      return Promise.resolve({
        body: {
          success: true,
          data,
        },
      });
    };
  });
  afterAll(() => {
    jest.clearAllMocks();
  });
  describe('AddonSettings', () => {
    it('should render well', async () => {
      let wrapper;
      await act(async () => {
        // render(<AddonSettings />, container);
        wrapper = mount(<AddonSettings />);
      });
      wrapper.update();
      expect(wrapper.find('PureAddonSettings').prop('addonConfig')).toStrictEqual(data);
      expect(wrapper.find('PureAddonSettings').prop('insId')).toBe(insId);
    });
  });
  describe('PureAddonSettings', () => {
    const config = {
      name: 'erda',
      org: 'erda.cloud',
    };
    it('should render well', async () => {
      const wrapper = mount(<PureAddonSettings insId={insId} />);
      expect(wrapper).toBeEmptyRender();
      wrapper.setProps({
        addonConfig: {
          config,
          canDel: false,
        },
      });
      wrapper.update();
      expect(wrapper.find('.param-k')).toHaveLength(2);
      expect(wrapper.find('.settings-delete')).not.toExist();
      wrapper.setProps({
        addonConfig: {
          config,
          canDel: true,
        },
      });
      wrapper.update();
      expect(wrapper.find('.settings-delete')).toExist();
      await wrapper.find('[deleteItem="service"]').prop('onConfirm')();
      expect(fn).toHaveBeenCalledTimes(1);
    });
  });
});
