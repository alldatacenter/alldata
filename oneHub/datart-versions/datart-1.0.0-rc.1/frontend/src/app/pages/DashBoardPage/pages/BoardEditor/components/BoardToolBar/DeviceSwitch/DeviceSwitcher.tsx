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
import { DesktopOutlined, MobileOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Tooltip } from 'antd';
import { ToolbarButton } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { DeviceType } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { dispatchResize } from 'app/utils/dispatchResize';
import { useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { editDashBoardInfoActions } from '../../../slice';
import { selectDeviceType } from '../../../slice/selectors';

export const DeviceSwitcher = () => {
  const t = useI18NPrefix(`viz.board.action`);
  const curDeviceType = useSelector(selectDeviceType);
  const dispatch = useDispatch();

  //why use async/await because: Wait until the state changes before triggering resize
  const onDeviceSwitch = async value => {
    const deviceType = value.key as DeviceType;
    await dispatch(editDashBoardInfoActions.changeBoardDevice(deviceType));
    dispatchResize();
  };

  const curIcon = useMemo(() => {
    return curDeviceType === DeviceType.Mobile ? (
      <MobileOutlined />
    ) : (
      <DesktopOutlined />
    );
  }, [curDeviceType]);
  const deviceItems = (
    <Menu onClick={onDeviceSwitch}>
      <Menu.Item
        disabled={curDeviceType === DeviceType.Desktop}
        key={DeviceType.Desktop}
        icon={<DesktopOutlined />}
      >
        {DeviceType.Desktop}
      </Menu.Item>
      {/* <Menu.Item key={'tablet'} icon={<TabletOutlined />}>
        {'tablet'}
      </Menu.Item> */}
      <Menu.Item
        disabled={curDeviceType === DeviceType.Mobile}
        key={DeviceType.Mobile}
        icon={<MobileOutlined />}
      >
        {DeviceType.Mobile}
      </Menu.Item>
    </Menu>
  );

  return (
    <Dropdown overlay={deviceItems} placement="bottomLeft" trigger={['click']}>
      <Tooltip title={t('deviceSwitch')}>
        <ToolbarButton icon={curIcon} />
      </Tooltip>
    </Dropdown>
  );
};
