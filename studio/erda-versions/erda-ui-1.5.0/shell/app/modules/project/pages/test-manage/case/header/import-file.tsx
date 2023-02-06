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

import React, { useState } from 'react';
import i18n, { isZh } from 'i18n';
import { Button, message } from 'antd';

import { FileSelect, FormModal } from 'common';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';
import './import-file.scss';

interface IProps {
  afterImport?: () => void;
}

const ImportFile = ({ afterImport }: IProps) => {
  const [uploadVisible, setUploadVisible] = useState(false);
  const { importTestCase } = testCaseStore.effects;
  const [confirmLoading] = useLoading(testCaseStore, ['importTestCase']);

  const locale = isZh() ? 'zh' : 'en';

  const handleCancel = () => {
    setUploadVisible(false);
  };

  const onSuccess = () => {
    message.success(i18n.t('dop:start importing, please view detail in records'));
    afterImport && afterImport();
    handleCancel();
  };

  const handleOk = (values: any) => {
    importTestCase(values).then((res: any) => {
      onSuccess(res);
    });
  };

  const toggleFileUpload = () => {
    setUploadVisible(!uploadVisible);
  };

  const fieldList = [
    {
      label: i18n.t('dop:select a document'),
      name: 'file',
      getComp: () => (
        <FileSelect
          accept=".xlsx, .xls, .XLSX, .XLS, .xmind, .xmt, .xmap, .xmind, .xmt, .xmap"
          visible={uploadVisible}
        />
      ),
    },
  ];

  return (
    <>
      <Button type="primary" ghost onClick={toggleFileUpload}>
        {i18n.t('import')}
      </Button>
      <FormModal
        loading={confirmLoading}
        okButtonState={confirmLoading}
        title={i18n.t('dop:upload files')}
        fieldsList={fieldList}
        visible={uploadVisible}
        onOk={handleOk}
        onCancel={handleCancel}
      >
        <div className="modal-tip">
          1.{i18n.t('dop:currently supports importing Xmind and Excel files')}
          <p className="my-3">
            &nbsp;&nbsp;{i18n.t('dop:if you need to import with Excel, please')}
            <a href={`/static/usecase_model_${locale}.xlsx`} className="modal-tip-link">
              {i18n.t('dop:download template')}
            </a>
            ；
          </p>
          <p className="mb-3">
            &nbsp;&nbsp;{i18n.t('dop:if you want to import with XMind, please')}
            <a href={`/static/usecase_model_${locale}.xmind`} className="modal-tip-link">
              {i18n.t('dop:download template')}
            </a>
            。
          </p>
        </div>
        <div className="modal-tip">2.{i18n.t('dop:xmind-import-tip')}</div>
      </FormModal>
    </>
  );
};

export default ImportFile;
