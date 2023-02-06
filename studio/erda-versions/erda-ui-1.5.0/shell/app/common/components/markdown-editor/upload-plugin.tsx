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
import { Popover, message, Upload } from 'antd';
import { Icon as CustomIcon } from 'common';
import { getCookies, getOrgFromPath } from 'common/utils';
import i18n from 'i18n';
import './upload-plugin.scss';

const UploadPlugin = (props: any) => {
  let hideLoading: any;
  const getUploadProps = (isImage?: boolean) => ({
    action: '/api/files',
    showUploadList: false,
    headers: {
      'OPENAPI-CSRF-TOKEN': getCookies('OPENAPI-CSRF-TOKEN'),
      org: getOrgFromPath(),
    },
    beforeUpload: (file: any) => {
      const UPLOAD_SIZE_LIMIT = 20; // M
      const isLtSize = file.size / 1024 / 1024 < UPLOAD_SIZE_LIMIT;
      if (!isLtSize) {
        message.warning(i18n.t('common:the uploaded file must not exceed {size}M', { size: UPLOAD_SIZE_LIMIT }));
      }
      return isLtSize;
    },
    onChange: ({ file }: any) => {
      const { status, response } = file;
      if (status === 'uploading' && !hideLoading) {
        hideLoading = message.loading(`${i18n.t('uploading')}...`, 0);
      }
      if (!response) {
        return;
      }
      hideLoading && hideLoading();
      hideLoading = undefined;
      const { success, err, data } = response;
      if (!success) {
        message.error(err.msg);
      } else {
        const { name, size, url } = data;
        props.editor.insertText(`\n${isImage ? '!' : ''}[${name}（${size}）](${url})\n`);
      }
    },
  });

  return (
    <Popover
      key="yf"
      title={i18n.t('common:add annex')}
      content={
        <div className="upload-plugin-menu">
          <Upload accept=".jpg, .jpeg, .png, .gif" {...getUploadProps(true)}>
            <div className="upload-item hover-active-bg">{i18n.t('common:image upload')}</div>
          </Upload>
          <Upload {...getUploadProps()}>
            <div className="upload-item hover-active-bg">{i18n.t('common:file upload')}</div>
          </Upload>
        </div>
      }
    >
      <span className="button button-type-annex" title="annex">
        <CustomIcon type="fujian" style={{ lineHeight: 'unset', marginRight: 0 }} />
      </span>
    </Popover>
  );
};
// 如果需要的话，可以在这里定义默认选项
UploadPlugin.defaultConfig = {};
UploadPlugin.align = 'right';
UploadPlugin.pluginName = 'upload';

export default UploadPlugin;
