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

import { createStore } from 'core/cube';
import { getOverview, getECSTrending } from '../services/cloud-overview';

interface IState {
  overviewData: CLOUD_OVERVIEW.OverviewData;
  ecsTrendingData: CLOUD_OVERVIEW.ECSTrendingData;
}
const initState: IState = {
  ecsTrendingData: {} as CLOUD_OVERVIEW.ECSTrendingData,
  overviewData: {
    COMPUTE: {
      resourceTypeData: {
        ECS: {
          totalCount: 0,
          displayName: '',
        },
      },
    },
    NETWORK: {
      resourceTypeData: {
        VPC: {
          totalCount: 0,
          displayName: '',
        },
        VSWITCH: {
          totalCount: 0,
          displayName: '',
        },
      },
    },
    STORAGE: {
      resourceTypeData: {
        OSS_BUCKET: {
          totalCount: 0,
          displayName: '',
        },
      },
    },
    CLOUD_SERVICE: {
      resourceTypeData: {
        REDIS: {
          totalCount: 0,
          displayName: '',
        },
        ROCKET_MQ: {
          totalCount: 0,
          displayName: '',
        },
        RDS: {
          totalCount: 0,
          displayName: '',
        },
      },
    },
  },
};
const cloudSource = createStore({
  name: 'cloudSource',
  state: initState,
  effects: {
    async getOverview({ call, update }, payload: CLOUD_OVERVIEW.Querys = {}) {
      const overviewData = await call(getOverview, payload);
      update({ overviewData });
    },
    async getECSTrending({ call, update }) {
      const ecsTrendingData = await call(getECSTrending);
      update({ ecsTrendingData });
    },
  },
  reducers: {
    clearOverviewData(state) {
      state.overviewData = initState.overviewData;
    },
  },
});

export default cloudSource;
