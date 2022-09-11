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

import React, { useState, useEffect } from 'react';
import { Button, Input, Space } from 'antd';
import type { InputProps } from 'antd/es/input';
import request from '@/utils/request';
import { useTranslation } from 'react-i18next';
import MyBusinessModal from './MyBusinessModal';

export interface Props extends Omit<InputProps, 'onChange'> {
  value?: string;
  onChange?: (value: string, record: Record<string, unknown>) => void;
  onSelect?: (value: Record<string, any>) => void;
}

const Comp: React.FC<Props> = ({ value, onChange, onSelect, ...rest }) => {
  const { t } = useTranslation();

  const [data, setData] = useState(value);

  const [myBusinessModal, setMyBusinessModal] = useState({
    visible: false,
  });

  useEffect(() => {
    if (value !== data) {
      setData(value);
    }
    // eslint-disable-next-line
  }, [value]);

  const triggerChange = (newData, record) => {
    if (onChange) {
      onChange(newData, record);
    }
  };

  const onSelectRow = (rowValue, record) => {
    setData(rowValue);
    triggerChange(rowValue, record);
  };

  const onTextChange = async value => {
    setData(value);

    const bussinessData = await request(`/group/get/${value}`);
    if (bussinessData) {
      triggerChange(value, bussinessData);
    }
  };

  return (
    <>
      <Space>
        <Input value={data} onChange={e => onTextChange(e.target.value)} {...rest} />
        <Button type="link" onClick={() => setMyBusinessModal({ visible: true })}>
          {t('components.ConsumeHelper.BusinessSelect.Search')}
        </Button>
      </Space>

      <MyBusinessModal
        {...myBusinessModal}
        visible={myBusinessModal.visible}
        onOk={(value, record) => {
          onSelectRow(value, record);
          if (onSelect) {
            onSelect(record);
          }
          setMyBusinessModal({ visible: false });
        }}
        onCancel={() => setMyBusinessModal({ visible: false })}
      />
    </>
  );
};

export default Comp;
