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

import { DOC_PREFIX, DOC_PROJECT_RESOURCE_MANAGE, WORKSPACE_LIST } from '../constants';

describe('emoji', () => {
  it('should Data normal', () => {
    expect(WORKSPACE_LIST.length).toBe(4);
    expect(DOC_PROJECT_RESOURCE_MANAGE).toBe(`${DOC_PREFIX}/manual/deploy/resource-management.html#管理配额`);
  });
});
