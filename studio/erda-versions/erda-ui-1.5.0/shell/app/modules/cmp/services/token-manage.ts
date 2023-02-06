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
interface TokenInfo {
  id: string;
  accessKey: string;
}

interface TokenParams {
  clusterName: string;
}
interface CreateAndResetTokenData {
  data: string;
}

const apis = {
  getToken: {
    api: `get@/api/cluster/credential/access-keys`,
  },
  createToken: {
    api: 'post@/api/cluster/credential/access-keys',
  },
  resetToken: {
    api: 'post@/api/cluster/credential/access-keys/actions/reset',
  },
};

export const getToken = apiCreator<(payload: TokenParams) => TokenInfo>(apis.getToken);
export const createToken = apiCreator<(payload: TokenParams) => CreateAndResetTokenData>(apis.createToken);
export const resetToken = apiCreator<(payload: TokenParams) => CreateAndResetTokenData>(apis.resetToken);
