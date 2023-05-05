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

import './index.scss';

import { Button, Upload } from 'antd';
import React, { useEffect, useState } from 'react';
import i18n from 'i18n';

import classnames from 'classnames';

interface IProps {
  visible: boolean;
  accept: string;
  onChange?: (file: any) => any;
  beforeUpload?: (file: any) => any;
}

const FileSelect = ({ accept, onChange, visible, beforeUpload }: IProps) => {
  const [fileData, setFileData] = useState(null);

  useEffect(() => {
    if (!visible) {
      setFileData(null);
    }
  }, [visible]);

  const customRequest = ({ file }: any) => {
    setFileData(file);
    onChange && onChange(file);
  };

  const handleBeforeUpload = (file: any) => {
    if (beforeUpload) {
      return beforeUpload(file);
    }
    return true;
  };

  return (
    <Upload accept={accept} showUploadList={false} customRequest={customRequest} beforeUpload={handleBeforeUpload}>
      <Button className={classnames({ placeholder: !fileData, 'upload-file-tip': true })}>
        {fileData ? fileData.name : i18n.t('common:Please select the file to be uploaded')}
      </Button>
    </Upload>
  );
};

export default FileSelect;
