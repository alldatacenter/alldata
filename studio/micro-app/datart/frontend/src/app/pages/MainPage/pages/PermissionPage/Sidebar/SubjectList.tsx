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

import { LoadingOutlined } from '@ant-design/icons';
import { List } from 'antd';
import { ListItem } from 'app/components';
import { useDebouncedSearch } from 'app/hooks/useDebouncedSearch';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo } from 'react';
import styled from 'styled-components/macro';
import { SubjectTypes } from '../constants';
import { DataSourceViewModel } from '../slice/types';
import { Searchbar } from './Searchbar';
import { PanelsProps } from './types';

interface SubjectListProps {
  viewpointId: string | undefined;
  viewpointType: SubjectTypes | undefined;
  dataSource: DataSourceViewModel[] | undefined;
  loading: boolean;
  onToDetail: PanelsProps['onToDetail'];
}

export const SubjectList = memo(
  ({
    viewpointId,
    viewpointType,
    dataSource,
    loading,
    onToDetail,
  }: SubjectListProps) => {
    const t = useI18NPrefix('permission');

    const { filteredData, debouncedSearch } = useDebouncedSearch(
      dataSource,
      (keywords, d) => d.name.includes(keywords),
    );
    return (
      <>
        <Searchbar placeholder={t('search')} onSearch={debouncedSearch} />
        <List
          loading={{
            spinning: loading,
            indicator: <LoadingOutlined />,
          }}
        >
          {filteredData?.map(({ id, name, type }) => (
            <StyledListItem
              key={id}
              selected={viewpointId === id && viewpointType === type}
              onClick={onToDetail(id, type)}
            >
              <List.Item.Meta title={name} />
            </StyledListItem>
          ))}
        </List>
      </>
    );
  },
);

const StyledListItem = styled(ListItem)`
  padding-left: ${14 + 8 + 16}px;
`;
