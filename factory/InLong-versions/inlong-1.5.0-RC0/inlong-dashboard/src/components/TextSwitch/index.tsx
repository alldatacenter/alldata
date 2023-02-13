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
import i18n from '@/i18n';
import { Button } from 'antd';
import { MinusOutlined, PlusOutlined } from '@/components/Icons';

export interface Props {
  value?: boolean;
  onChange?: (value: boolean) => void;
  title?: string;
}

const TextSwitch: React.FC<Props> = ({
  value = false,
  onChange,
  title = i18n.t('components.TextSwitch.Title'),
}) => {
  const [currentValue, setCurrentValue] = useState(false);

  useEffect(() => {
    if (value !== currentValue) {
      setCurrentValue(value);
    }
    // eslint-disable-next-line
  }, [value]);

  const onToggle = newValue => {
    setCurrentValue(newValue);
    if (onChange && newValue !== value) {
      onChange(newValue);
    }
  };

  return (
    <Button type="link" onClick={() => onToggle(!currentValue)} style={{ padding: 0 }}>
      {value ? <MinusOutlined /> : <PlusOutlined />}
      {title}
    </Button>
  );
};

export default TextSwitch;
