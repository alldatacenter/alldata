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

import { message } from 'antd';
import { getCookies } from '.';
import { qs, getOrgFromPath } from 'common/utils';
import i18n from 'i18n';
import { UploadProps } from 'core/common/interface';

/**
 * 获得公共上传props
 * @param overwrite 覆盖配置
 * @param sizeLimit 上传大小限制，单位M
 */
export function getUploadProps(
  overwrite: Merge<UploadProps, { queryData?: Record<string, any> }>,
  sizeLimit = 20,
): UploadProps {
  const { queryData = {}, ...rest } = overwrite;
  const queryStr = qs.stringify(queryData);
  return {
    action: `/api/files${queryStr ? `?${queryStr}` : ''}`,
    showUploadList: false,
    headers: {
      'OPENAPI-CSRF-TOKEN': getCookies('OPENAPI-CSRF-TOKEN'),
      org: getOrgFromPath(),
    },
    beforeUpload: (file: any) => {
      const UPLOAD_SIZE_LIMIT = sizeLimit; // M
      const isLtSize = file.size / 1024 / 1024 < UPLOAD_SIZE_LIMIT;
      if (!isLtSize) {
        message.warning(i18n.t('common:the uploaded file must not exceed {size}M', { size: UPLOAD_SIZE_LIMIT }));
      }
      return isLtSize;
    },
    ...rest,
  };
}
