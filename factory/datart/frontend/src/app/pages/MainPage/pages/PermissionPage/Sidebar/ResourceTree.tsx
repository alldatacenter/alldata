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

import { Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useSearchAndExpand } from 'app/hooks/useSearchAndExpand';
import { memo, useCallback, useEffect, useMemo } from 'react';
import { listToTree } from 'utils/utils';
import { ResourceTypes, SubjectTypes } from '../constants';
import { DataSourceTreeNode, DataSourceViewModel } from '../slice/types';
import { Searchbar } from './Searchbar';

interface ResourceTreeProps {
  loading: boolean;
  dataSource: DataSourceViewModel[] | undefined;
  onSelect: (id: string, type: SubjectTypes | ResourceTypes) => () => void;
}

export const ResourceTree = memo(
  ({ loading, dataSource, onSelect }: ResourceTreeProps) => {
    const t = useI18NPrefix('permission');

    const treeData = useMemo(
      () => listToTree(dataSource, null, []) as DataSourceTreeNode[],
      [dataSource],
    );

    const {
      filteredData,
      expandedRowKeys,
      onExpand,
      debouncedSearch,
      setExpandedRowKeys,
    } = useSearchAndExpand(treeData, (keywords, d) =>
      d.name.includes(keywords),
    );

    useEffect(() => {
      if (treeData && treeData.length > 0) {
        setExpandedRowKeys([treeData[0].key as string]);
      }
    }, [treeData, setExpandedRowKeys]);

    const treeSelect = useCallback(
      (_, { node, nativeEvent }) => {
        nativeEvent.stopPropagation();
        const { id, type } = node as DataSourceTreeNode;
        onSelect(id, type)();
      },
      [onSelect],
    );

    const renderTreeTitle = useCallback(({ id, title }) => {
      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
        </TreeTitle>
      );
    }, []);

    return (
      <>
        <Searchbar
          placeholder={t('searchResources')}
          onSearch={debouncedSearch}
        />
        <Tree
          loading={loading}
          treeData={filteredData}
          titleRender={renderTreeTitle}
          expandedKeys={expandedRowKeys}
          icon={false}
          onSelect={treeSelect}
          onExpand={onExpand}
        />
      </>
    );
  },
);
