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
import { Select, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo, useCallback, useEffect, useState } from 'react';
import styled from 'styled-components/macro';

export interface AssistViewFieldsProps {
  onChange?: (value: string[], type) => void;
  value?: string[];
  isHierarchyTree?: boolean;
  style: object;
  viewList;
  viewFieldList;
}
export const AssistViewFields: React.FC<AssistViewFieldsProps> = memo(
  ({
    onChange,
    value: propsValue,
    isHierarchyTree,
    style,
    viewList,
    viewFieldList,
  }) => {
    const tc = useI18NPrefix(`viz.control`);
    const [val, setVal] = useState<string[]>([]);

    const handleOnChange = useCallback(
      (value, type) => {
        setVal(value);
        onChange?.(value || [], type);
      },
      [onChange],
    );

    useEffect(() => {
      setVal(propsValue || []);
    }, [onChange, propsValue]);

    return (
      <StyleSpace style={style}>
        <Select
          optionFilterProp={'label'}
          showSearch
          placeholder={tc('selectView')}
          value={val?.[0]}
          onChange={value => {
            handleOnChange([value], 'view');
          }}
          options={viewList}
        ></Select>
        {!isHierarchyTree && (
          <Select
            optionFilterProp={'label'}
            onChange={value => {
              handleOnChange([val?.[0] || '', value], 'viewField');
            }}
            placeholder={tc('selectViewField')}
            showSearch
            value={val?.[1]}
            loading={!viewFieldList}
            options={viewFieldList}
          ></Select>
        )}
      </StyleSpace>
    );
  },
);

const StyleSpace = styled(Space)`
  display: flex;
  > div {
    flex: 1;
  }
`;
