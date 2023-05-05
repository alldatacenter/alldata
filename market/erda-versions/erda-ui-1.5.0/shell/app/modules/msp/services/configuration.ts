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

import { apiCreator } from 'core/service';

const apis = {
  getAcquisitionAndLang: {
    api: 'get@/api/msp/apm/instrumentation-library',
  },
  getInfo: {
    api: 'get@/api/msp/apm/instrumentation-library/config-docs',
  },
  getAllToken: {
    api: 'post@/api/msp/credential/access-keys/records',
  },
  createToken: {
    api: 'post@/api/msp/credential/access-keys',
  },
  getDetailToken: {
    api: 'get@/api/msp/credential/access-keys/:id',
  },
  deleteDetailToken: {
    api: 'delete@/api/msp/credential/access-keys/:id',
  },
};
export const getAcquisitionAndLang = apiCreator<() => CONFIGURATION.IStrategy[]>(apis.getAcquisitionAndLang);
export const getInfo = apiCreator<(payload: CONFIGURATION.IDocs) => string>(apis.getInfo);
export const getDetailToken = apiCreator<(payload: CONFIGURATION.IDelAndFindToken) => CONFIGURATION.IAllTokenData>(
  apis.getDetailToken,
);
export const deleteDetailToken = apiCreator<(payload: CONFIGURATION.IDelAndFindToken) => void>(apis.deleteDetailToken);
export const getAllToken = apiCreator<(payload: CONFIGURATION.IAllToken) => CONFIGURATION.ITokenList>(apis.getAllToken);
export const createToken = apiCreator<(payload: CONFIGURATION.ICreateKey) => CONFIGURATION.IAllTokenData>(
  apis.createToken,
);
