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
import routeInfoStore from 'core/stores/route';
import { goTo } from 'common/utils';

enum TRANSACTION_TYPE {
  web = 'http',
  overview = 'http',
  rpc = 'rpc',
  cache = 'cache',
  db = 'database',
}

const Transfer = () => {
  React.useEffect(() => {
    const [params] = routeInfoStore.getState((s) => [s.params]);
    const { applicationId, runtimeName, serviceName, type, ...rest } = params;
    const serviceId = window.encodeURIComponent(`${applicationId}_${runtimeName}_${serviceName}`);
    let url = goTo.pages.mspServiceTransaction;
    let query;
    if (['nodejs', 'jvm'].includes(type)) {
      url = goTo.pages.mspServiceProcess;
      query = undefined;
    } else {
      query = { type: TRANSACTION_TYPE[type] };
    }
    goTo(url, {
      ...rest,
      applicationId,
      serviceName,
      serviceId,
      query,
      replace: true,
    });
  }, []);
  return null;
};
export default Transfer;
