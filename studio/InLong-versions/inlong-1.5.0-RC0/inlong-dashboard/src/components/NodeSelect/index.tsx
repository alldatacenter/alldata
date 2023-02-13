/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import HighSelect, { HighSelectProps } from '@/components/HighSelect';
import i18n from '@/i18n';
import { Link } from 'react-router-dom';

export interface NodeSelectProps extends HighSelectProps {
  nodeType: string;
}

const NodeSelect: React.FC<NodeSelectProps> = _props => {
  const props: HighSelectProps = {
    ..._props,
    showSearch: true,
    allowClear: true,
    filterOption: false,
    options: {
      ..._props.options,
      requestTrigger: ['onOpen', 'onSearch'],
      requestService: keyword => ({
        url: '/node/list',
        method: 'POST',
        data: {
          keyword,
          type: _props.nodeType,
          pageNum: 1,
          pageSize: 20,
        },
      }),
      requestParams: {
        formatResult: result =>
          result?.list?.map(item => ({
            label: item.name,
            value: item.name,
          })),
      },
    },
    addonAfter: (
      <Link to="/node" target="_blank">
        {i18n.t('components.NodeSelect.Create')}
      </Link>
    ),
  };
  return <HighSelect {...props} />;
};

export default NodeSelect;
