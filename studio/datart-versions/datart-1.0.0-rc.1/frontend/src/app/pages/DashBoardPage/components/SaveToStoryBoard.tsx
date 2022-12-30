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

import { List, Modal } from 'antd';
import { ListItem } from 'app/components';
import { getCascadeAccess } from 'app/pages/MainPage/Access';
import {
  PermissionLevels,
  ResourceTypes,
  VizResourceSubTypes,
} from 'app/pages/MainPage/pages/PermissionPage/constants';
import { selectStoryboards } from 'app/pages/MainPage/pages/VizPage/slice/selectors';
import { Storyboard } from 'app/pages/MainPage/pages/VizPage/slice/types';
import {
  selectIsOrgOwner,
  selectPermissionMap,
} from 'app/pages/MainPage/slice/selectors';
import { FC, memo, useCallback, useState } from 'react';
import { useSelector } from 'react-redux';
import { getPath } from 'utils/utils';

interface SaveToStoryBoardTypes {
  isModalVisible: boolean;
  handleOk: (id) => void;
  handleCancel: () => void;
  title: string;
  orgId: string;
}

const SaveToStoryBoard: FC<SaveToStoryBoardTypes> = memo(
  ({ isModalVisible, handleOk, handleCancel, title }) => {
    const [selectId, setSelectId] = useState<string>('');
    const isOwner = useSelector(selectIsOrgOwner);
    const permissionMap = useSelector(selectPermissionMap);

    const filterStoryData = useCallback(
      vizData => {
        let dashboardIds: any = [];
        let dashboardData = vizData?.filter(v => {
          const path = getPath(
            vizData as Array<{ id: string; parentId: string }>,
            { id: v.id, parentId: v.parentId },
            VizResourceSubTypes.Folder,
          );

          const AllowManage = getCascadeAccess(
            isOwner,
            permissionMap,
            ResourceTypes.Viz,
            path,
            PermissionLevels.Manage,
          );

          return AllowManage;
        });

        const FileData = vizData?.filter(v => {
          return dashboardIds.indexOf(v.id) !== -1;
        });

        return FileData.concat(dashboardData);
      },
      [isOwner, permissionMap],
    );

    const selectStoryBoard = useCallback(storyData => {
      setSelectId(storyData.id);
    }, []);

    const saveToStoryFn = useCallback(
      async selectId => {
        handleOk(selectId);
      },
      [handleOk],
    );

    const storyData: Storyboard[] = filterStoryData(
      useSelector(selectStoryboards),
    );
    return (
      <Modal
        title={title}
        visible={isModalVisible}
        onOk={() => {
          saveToStoryFn(selectId);
        }}
        onCancel={handleCancel}
        okButtonProps={{ disabled: !selectId }}
      >
        <List
          dataSource={storyData}
          renderItem={s => (
            <ListItem
              onClick={() => selectStoryBoard(s)}
              selected={selectId === s.id}
            >
              <List.Item.Meta title={s.name} />
            </ListItem>
          )}
        ></List>
      </Modal>
    );
  },
);

export default SaveToStoryBoard;
