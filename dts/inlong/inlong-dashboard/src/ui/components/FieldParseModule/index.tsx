/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, { useState } from 'react';
import { Button, Divider, Input, message, Modal, Radio, Space, Table, Upload } from 'antd';
import type { UploadProps } from 'antd';
import {
  CopyOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  DownloadOutlined,
  FileAddOutlined,
  FileExcelOutlined,
  FileOutlined,
  ForkOutlined,
  FormOutlined,
  PlayCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import { useRequest } from '@/ui/hooks';
import { useTranslation } from 'react-i18next';
import { config } from '@/configs/default';

export interface RowType {
  fieldName: string;
  fieldType: string;
  fieldComment: string;
}

interface FieldParseModuleProps {
  visible: boolean;
  onOverride: (fields: RowType[]) => void;
  onAppend: (fields: RowType[]) => void;
  onHide: () => void; // added onHide callback
}

const FieldParseModule: React.FC<FieldParseModuleProps> = ({
  onOverride,
  onAppend,
  visible,
  onHide,
}) => {
  const { t } = useTranslation();

  const [selectedFormat, setSelectedFormat] = useState('json');

  const [statement, setStatement] = useState('');
  const [previewData, setPreviewData] = useState<RowType[]>([]);

  const handleCancel = () => {
    onHide(); // call onHide callback when closing module
  };

  const handleFormat = () => {
    switch (selectedFormat) {
      case 'json':
        setStatement(JSON.stringify(JSON.parse(statement), null, 2));
        break;
      case 'sql':
        setStatement(
          statement.replace(/(FROM|JOIN|WHERE|GROUP BY|HAVING|ORDER BY|LIMIT)/g, '\n$1'),
        );
        break;
      case 'csv':
        break;
      default:
        break;
    }
  };

  const handleAppend = () => {
    // Append output value to the original fields list
    onAppend([...previewData]);
    onHide();
  };
  const downloadTemplate = async () => {
    try {
      const response = await fetch(config.requestPrefix + '/stream/fieldsImportTemplate');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'InLong-stream-fields-template.xlsx');
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      console.error(error);
    }
  };

  const uploadProps: UploadProps = {
    name: 'file',
    action: config.requestPrefix + '/stream/parseFieldsByExcel',
    accept: '.xlsx',
    headers: {
      authorization: 'authorization-text',
    },
    beforeUpload: file => {
      if (file.type !== 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') {
        message.error(t('components.FieldParseModule.OnlyUploadExcelFile'));
        return false;
      }
      return true;
    },
    onChange(info) {
      if (info.file.status !== 'uploading') {
        console.log(`${info.file} file uploading.`);
      }
      if (info.file.status === 'done') {
        // upload success.
        let newPreviewData = info.file.response.data;
        setPreviewData(newPreviewData);
        console.log(`${info.file.name} file upload success.`);
      } else if (info.file.status === 'error') {
        console.log(`${info.file.name} file upload failed.`);
      }
    },
  };

  const handleOverride = () => {
    onOverride(previewData);
    onHide();
  };
  const { run: runParseFields } = useRequest(
    {
      url: '/stream/parseFields',
      method: 'POST',
      data: {
        method: selectedFormat,
        statement: statement,
      },
    },
    {
      manual: true,
      onSuccess: res => {
        console.log('parse fields success.');
        setPreviewData(res);
      },
    },
  );

  const columns = [
    {
      title: 'Name',
      dataIndex: 'fieldName',
      key: 'fieldName',
    },
    {
      title: 'Type',
      dataIndex: 'fieldType',
      key: 'fieldType',
    },
    {
      title: 'Description',
      dataIndex: 'fieldComment',
      key: 'fieldComment',
    },
  ];

  function onPasta() {
    setPreviewData(null);
    switch (selectedFormat) {
      case 'json':
        setStatement(`[
  {
    "name": "user_name",
    "type": "string",
    "desc": "the name of user"
  },
  {
    "name": "user_age",
    "type": "int",
    "desc": "the age of user"
  }
]`);
        break;
      case 'sql':
        setStatement(`CREATE TABLE Persons
                              (
                                  user_name int comment 'the name of user',
                                  user_age  varchar(255) comment 'the age of user'
                              )`);
        break;
      case 'csv':
        setStatement(`user_name,string,name of user
user_age,int,age of user`);
        break;
      default:
        break;
    }
  }

  return (
    <>
      <Modal
        key={'field-parse-module'}
        title={
          <>
            <FileAddOutlined />
            {t('components.FieldParseModule.BatchAddField')}
          </>
        }
        open={visible}
        onCancel={handleCancel}
        footer={[
          <Space key="footer_space" size={'small'} style={{ width: '100%' }} direction={'vertical'}>
            <Space key={'footer_content_space'}>
              <Button
                key={'doAppend'}
                type="primary"
                disabled={previewData === null || previewData.length === 0}
                onClick={handleAppend}
              >
                {t('components.FieldParseModule.Append')}
              </Button>
              <Button
                key={'doOverwrite'}
                type="primary"
                disabled={previewData === null || previewData.length === 0}
                onClick={handleOverride}
              >
                {t('components.FieldParseModule.Override')}
              </Button>
            </Space>
          </Space>,
        ]}
      >
        <div>
          <Radio.Group
            key={'mode_radio_group'}
            onChange={e => setSelectedFormat(e.target.value)}
            value={selectedFormat}
            style={{ marginBottom: 6 }}
          >
            <Radio.Button
              key={'module_json'}
              value="json"
              onClick={() => {
                setPreviewData(null);
              }}
            >
              <ForkOutlined />
              JSON
            </Radio.Button>
            <Radio.Button
              key={'module_sql'}
              value="sql"
              onClick={() => {
                setPreviewData(null);
              }}
            >
              <DatabaseOutlined />
              SQL
            </Radio.Button>
            <Radio.Button
              key={'module_csv'}
              value="csv"
              onClick={() => {
                setPreviewData(null);
              }}
            >
              <FileOutlined />
              CSV
            </Radio.Button>
            <Radio.Button
              key={'module_excel'}
              value="excel"
              onClick={() => {
                setPreviewData(null);
              }}
            >
              <FileExcelOutlined />
              Excel
            </Radio.Button>
          </Radio.Group>
        </div>
        <div>
          {['json', 'sql', 'csv'].includes(selectedFormat) && (
            <Input.TextArea
              key={'statement_content'}
              rows={16}
              value={statement}
              onChange={e => setStatement(e.target.value)}
            />
          )}
        </div>
        <div>
          {selectedFormat !== 'excel' && (
            <>
              <Button
                key={'format_button'}
                icon={<FormOutlined />}
                onClick={handleFormat}
                disabled={statement?.length === 0}
                size={'small'}
              >
                {t('components.FieldParseModule.Format')}
              </Button>
              <Button
                key={'clear_button'}
                onClick={() => {
                  setStatement('');
                  setPreviewData(null);
                }}
                icon={<DeleteOutlined />}
                disabled={statement?.length === 0}
                size={'small'}
              >
                {t('components.FieldParseModule.Empty')}
              </Button>
              <Button key={'pasta_button'} onClick={onPasta} icon={<CopyOutlined />} size={'small'}>
                {t('components.FieldParseModule.PasteTemplate')}
              </Button>
              <Divider key={'divider_button'} type={'vertical'} />
              <Button
                key={'parse_button'}
                type="primary"
                onClick={runParseFields}
                icon={<PlayCircleOutlined />}
                disabled={statement?.length === 0}
              >
                {t('components.FieldParseModule.Parse')}
              </Button>
            </>
          )}
        </div>

        {selectedFormat === 'excel' && (
          <div>
            <Upload {...uploadProps}>
              <Button key="upload" type={'primary'} icon={<UploadOutlined />} size={'small'}>
                {t('components.FieldParseModule.Upload')}
              </Button>
            </Upload>
            <Button
              key="downloadTemplate"
              onClick={downloadTemplate}
              icon={<DownloadOutlined />}
              size={'small'}
            >
              {t('components.FieldParseModule.DownloadTemplate')}
            </Button>
          </div>
        )}

        <div>
          <Table
            key="previewTable"
            rowKey="name"
            columns={columns}
            dataSource={previewData}
            pagination={false}
          />
        </div>
      </Modal>
    </>
  );
};

export default FieldParseModule;
