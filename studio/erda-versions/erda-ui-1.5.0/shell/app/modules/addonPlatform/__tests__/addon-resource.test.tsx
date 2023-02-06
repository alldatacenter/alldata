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
import AddonResource from 'addonPlatform/pages/addon-resource/addon-resource';
import routeInfoStore from 'core/stores/route';
import addonStore from 'common/stores/addon';
import metricsMonitorStore from 'common/stores/metrics';
import { mount, shallow } from 'enzyme';

const insId = 123;
const routerData = {
  params: {
    insId,
  },
};

const info = {
  addonDetail: {
    addonName: 'mySql',
    cluster: 'ERDA',
    realInstanceId: 'addon-sql',
  },
};

describe('AddonResource', () => {
  beforeAll(() => {
    jest.mock('core/stores/route');
    jest.mock('common/stores/metrics');
    metricsMonitorStore.effects.listMetricByResourceType = jest.fn();
    routeInfoStore.getState = (fn) => {
      return fn(routerData);
    };
    routeInfoStore.useStore = (fn) => {
      return fn(routerData);
    };
    addonStore.useStore = (fn) => {
      return fn(info);
    };
  });
  afterAll(() => {
    jest.resetAllMocks();
  });
  it('should call getAddonDetail', () => {
    const getAddonDetail = jest.fn();
    addonStore.getAddonDetail = getAddonDetail;
    mount(<AddonResource />);
    expect(getAddonDetail).toHaveBeenLastCalledWith(insId);
  });
  it('should render well', () => {
    const getAddonDetail = jest.fn();
    addonStore.getAddonDetail = getAddonDetail;
    const wrapper = shallow(<AddonResource />);
    expect(wrapper.find('AddonResource').prop('resourceInfo')).toStrictEqual(info.addonDetail);
    expect(wrapper.find('MonitorChartPanel').prop('resourceType')).toBe(info.addonDetail.addonName);
  });
  it('resourceInfo is empty', () => {
    const wrapper = shallow(<AddonResource resourceInfo={{}} />);
    expect(wrapper).toBeEmptyRender();
  });
  it('resourceInfo is not empty', () => {
    const resourceInfo = {
      createdAt: '2021-04-24 12:12:12',
      reference: 'erda',
      workspace: 'STAGING',
      addonName: 'ERDA',
      plan: 'professional',
      cluster: 'erda cloud',
      version: '1.0.0',
    };
    const wrapper = mount(<AddonResource resourceInfo={resourceInfo} />);
    expect(wrapper.find('.info-key')).toHaveLength(7);
  });
});
