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
import {
  BranchesOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  FullscreenOutlined,
  GroupOutlined,
  InfoOutlined,
  LinkOutlined,
  LockOutlined,
  SyncOutlined,
  UngroupOutlined,
  UnlockOutlined,
} from '@ant-design/icons';
import { VizRenderMode } from '../pages/Board/slice/types';
import { WidgetActionListItem, widgetActionType } from '../types/widgetTypes';

export const useWidgetDropdownList = (
  renderMode: VizRenderMode,
  props: WidgetActionListItem<widgetActionType>[],
) => {
  // const t = useI18NPrefix(`viz.widget.action`);
  const allWidgetActionList: WidgetActionListItem<widgetActionType>[] = [
    {
      key: 'refresh',
      label: 'refresh',
      icon: <SyncOutlined />,
    },
    {
      key: 'fullScreen',
      label: 'fullScreen',
      icon: <FullscreenOutlined />,
    },
    {
      key: 'edit',
      label: 'edit',
      icon: <EditOutlined />,
    },
    {
      key: 'delete',
      label: 'delete',
      icon: <DeleteOutlined />,
      danger: true,
    },

    {
      key: 'info',
      label: 'info',
      icon: <InfoOutlined />,
    },
    {
      key: 'lock',
      label: 'lock',
      icon: <LockOutlined />,
    },
    {
      key: 'unlock',
      label: 'unlock',
      icon: <UnlockOutlined />,
    },
    {
      key: 'makeLinkage',
      label: 'makeLinkage',
      icon: <LinkOutlined />,
      divider: true,
    },
    {
      key: 'closeLinkage',
      label: 'closeLinkage',
      icon: <CloseCircleOutlined />,
      danger: true,
    },
    {
      key: 'makeJump',
      label: 'makeJump',
      icon: <BranchesOutlined />,
      divider: true,
    },
    {
      key: 'closeJump',
      label: 'closeJump',
      icon: <CloseCircleOutlined />,
      danger: true,
    },
    {
      key: 'group',
      label: 'group',
      icon: <GroupOutlined />,
    },
    {
      key: 'unGroup',
      label: 'unGroup',
      icon: <UngroupOutlined />,
    },
  ];
  const widgetDropdownList = allWidgetActionList
    .map(item => {
      const newItem = props?.find(item2 => item2.key === item.key);
      if (newItem) {
        item.show = newItem.hasOwnProperty('show') ? newItem.show : true;
        item.disabled = newItem.hasOwnProperty('disabled')
          ? newItem.disabled
          : item.disabled;
        item.label = newItem.hasOwnProperty('label')
          ? newItem.label
          : item.label;
        item.divider = newItem.hasOwnProperty('divider')
          ? newItem.divider
          : item.divider;
        item.renderMode = newItem.hasOwnProperty('renderMode')
          ? newItem.renderMode
          : item.renderMode;
      } else {
        item.show = false;
      }
      return item;
    })
    .filter(item => item.show && item.renderMode?.includes(renderMode));
  return widgetDropdownList;
};
