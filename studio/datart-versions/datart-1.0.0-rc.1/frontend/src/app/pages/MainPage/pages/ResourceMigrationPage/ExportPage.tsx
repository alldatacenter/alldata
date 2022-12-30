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
import { FC, memo, useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { listToTree } from 'utils/utils';
import { selectOrgId } from '../../slice/selectors';
import { usePermissionSlice } from '../PermissionPage/slice';
import { selectFolders } from '../PermissionPage/slice/selectors';
import { DataSourceTreeNode } from '../PermissionPage/slice/types';
import { getFolders } from '../VizPage/slice/thunks';
import { Folder } from '../VizPage/slice/types';
import { ExportSelector } from './ExportSelector';

export const ExportPage: FC<{}> = memo(() => {
  usePermissionSlice();
  const orgId = useSelector(selectOrgId);
  const dispatch = useDispatch();
  useEffect(() => {
    if (orgId) {
      dispatch(getFolders(orgId));
    }
  }, [dispatch, orgId]);
  const folders = useSelector(selectFolders);
  const treeData = useMemo(
    () => listToTree(folders, null, []) as DataSourceTreeNode[],
    [folders],
  );
  return (
    <ExportSelector
      treeData={treeData}
      folders={(folders || []) as unknown as Folder[]}
    />
  );
});
