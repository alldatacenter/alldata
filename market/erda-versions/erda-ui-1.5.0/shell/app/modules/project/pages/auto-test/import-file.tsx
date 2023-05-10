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
import i18n from 'i18n';
import { message } from 'antd';
import { FileSelect, FormModal } from 'common';
import { useLoading } from 'core/stores/loading';
import testCaseStore from 'project/stores/test-case';

interface IProps {
  visible: boolean;
  type?: 'testCase' | 'testCaseSet';
  onClose: () => void;
}

const ImportFile = ({ visible, onClose, type = 'testCase' }: IProps) => {
  const [uploadVisible, setUploadVisible] = useState(visible);
  const { importAutoTestCase, importAutoTestCaseSet } = testCaseStore.effects;
  const [testCaseLoading, testCaseSetLoading] = useLoading(testCaseStore, [
    'importAutoTestCase',
    'importAutoTestCaseSet',
  ]);

  const importMap = {
    testCase: { loading: testCaseLoading, fetch: importAutoTestCase },
    testCaseSet: { loading: testCaseSetLoading, fetch: importAutoTestCaseSet },
  };

  React.useEffect(() => {
    setUploadVisible(visible);
  }, [visible]);

  const onSuccess = () => {
    message.success(i18n.t('dop:start importing, please view detail in records'));
    onClose();
  };

  const handleOk = (values: { file: File }) => {
    importMap[type]?.fetch?.(values).then(() => {
      onSuccess();
    });
  };

  const fieldList = [
    {
      label: i18n.t('dop:select a document'),
      name: 'file',
      getComp: () => <FileSelect accept=".xlsx, .xls, .XLSX, .XLS" visible={uploadVisible} />,
    },
  ];
  const loading = importMap[type]?.loading;
  return (
    <FormModal
      loading={loading}
      okButtonState={loading}
      title={i18n.t('dop:upload files')}
      fieldsList={fieldList}
      visible={uploadVisible}
      onOk={handleOk}
      modalProps={{ getContainer: false }}
      onCancel={onClose}
    >
      <div>{i18n.t('dop:currently supports importing Excel files')}</div>
    </FormModal>
  );
};

export default ImportFile;
