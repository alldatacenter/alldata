/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Empty } from 'antd';
import { SchemaTable } from 'app/pages/MainPage/pages/ViewPage/components/SchemaTable';
import { Schema } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { useCallback } from 'react';
import styled from 'styled-components/macro';
interface SchemaProps {
  value?: Schema[];
  dataSource?: object[];
  onChange?: (val?: Schema[]) => void;
}

export function SchemaComponent({ value, dataSource, onChange }: SchemaProps) {
  const model = value
    ? value.reduce((m, c) => {
        m[c.name] = c;
        return m;
      }, {})
    : {};

  const schemaTypeChange = useCallback(
    name => keyPath => {
      onChange &&
        onChange(
          value?.map(v => (v.name === name ? { ...v, type: keyPath[0] } : v)),
        );
    },
    [value, onChange],
  );

  return value ? (
    <SchemaTable
      width={600}
      height={400}
      model={model}
      hierarchy={model}
      dataSource={dataSource}
      loading={false}
      hasCategory={false}
      pagination={false}
      hasFormat={false}
      onSchemaTypeChange={schemaTypeChange}
      bordered
    />
  ) : (
    <EmptyWrapper>
      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
    </EmptyWrapper>
  );
}

const EmptyWrapper = styled.div`
  border: 1px solid ${p => p.theme.borderColorBase};
`;
