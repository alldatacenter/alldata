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

import { getUploadProps } from '../../utils/upload-props';
import { getOrgFromPath } from 'common/utils';

describe('getUploadProps', () => {
  it('upload props should work fine', () => {
    const result = getUploadProps({});
    expect(result.action).toBe('/api/files');
    expect(result.headers).toStrictEqual({
      'OPENAPI-CSRF-TOKEN': 'OPENAPI-CSRF-TOKEN',
      org: getOrgFromPath(),
    });
    expect(result.beforeUpload({ size: 20971550 })).toBe(false);
    expect(document.querySelectorAll('.ant-message-notice').length).toBeTruthy();
    expect(result.beforeUpload({ size: 10 })).toBe(true);
  });
});
