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
import { ControlOutlined } from '@ant-design/icons';
import { Dropdown, Menu, Tooltip } from 'antd';
import { ToolbarButton } from 'app/components';
import { ControllerFacadeTypes } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useContext } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { G60 } from 'styles/StyleConstants';
import { ORIGINAL_TYPE_MAP } from '../../../../../constants';
import { addControllerAction } from '../../../slice/actions/controlActions';
import { selectHasQueryBtn, selectHasResetBtn } from '../../../slice/selectors';
import { BoardToolBarContext } from '../context/BoardToolBarContext';

export interface AddControlBtnProps {}
export interface ButtonItemType<T> {
  name?: string;
  icon: any;
  type: T;
  disabled?: boolean;
}
export const AddController: React.FC<AddControlBtnProps> = () => {
  const t = useI18NPrefix(`viz.board.action`);
  const tFilterName = useI18NPrefix(`viz.common.enum.controllerFacadeTypes`);
  const tType = useI18NPrefix(`viz.board.controlTypes`);
  const tWt = useI18NPrefix(`viz.widget.type`);
  const { boardId, boardType } = useContext(BoardToolBarContext);
  const dispatch = useDispatch();
  const hasQueryBtn = useSelector(selectHasQueryBtn);
  const hasResetBtn = useSelector(selectHasResetBtn);
  const onAddController = (info: { key: any }) => {
    dispatch(
      addControllerAction({
        type: info.key,
        boardId: boardId,
        boardType: boardType,
      }),
    );
  };
  const conventionalControllers: ButtonItemType<ControllerFacadeTypes>[] = [
    {
      icon: '',
      type: ControllerFacadeTypes.DropdownList,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.MultiDropdownList,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.RadioGroup,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.CheckboxGroup,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.Text,
    },
    // {
    //   name: '单选下拉树',
    //   icon: '',
    //   type: ControllerFacadeTypes.RadioGroup,
    //   disabled: false,
    // },
    {
      icon: '',
      type: ControllerFacadeTypes.DropDownTree,
    },
  ];
  const dateControllers: ButtonItemType<ControllerFacadeTypes>[] = [
    {
      icon: '',
      type: ControllerFacadeTypes.RangeTime,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.Time,
    },
  ];
  const numericalControllers: ButtonItemType<ControllerFacadeTypes>[] = [
    {
      icon: '',
      type: ControllerFacadeTypes.RangeValue,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.Value,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.Slider,
    },
    {
      icon: '',
      type: ControllerFacadeTypes.RangeSlider,
      disabled: true,
    },
  ];
  const buttons: ButtonItemType<any>[] = [
    {
      icon: '',
      type: ORIGINAL_TYPE_MAP.queryBtn,
      disabled: hasQueryBtn,
    },
    {
      icon: '',
      type: ORIGINAL_TYPE_MAP.resetBtn,
      disabled: hasResetBtn,
    },
  ];
  const renderTitle = (text: string) => {
    return <span style={{ color: G60, fontWeight: 500 }}>{text}</span>;
  };
  const controlerItems = (
    <Menu onClick={onAddController}>
      <Menu.ItemGroup
        key="conventionalControllers"
        title={renderTitle(tType('common'))}
      >
        {conventionalControllers.map(({ icon, type }) => (
          <Menu.Item key={type} icon={icon}>
            {tFilterName(type)}
          </Menu.Item>
        ))}
      </Menu.ItemGroup>
      <Menu.ItemGroup key="dateControllers" title={renderTitle(tType('date'))}>
        {dateControllers.map(({ icon, type }) => (
          <Menu.Item key={type} icon={icon}>
            {tFilterName(type)}
          </Menu.Item>
        ))}
      </Menu.ItemGroup>
      <Menu.ItemGroup
        key="numericalControllers"
        title={renderTitle(tType('numeric'))}
      >
        {numericalControllers.map(({ icon, type, disabled }) => (
          <Menu.Item key={type} icon={icon} disabled={disabled}>
            {tFilterName(type)}
          </Menu.Item>
        ))}
      </Menu.ItemGroup>
      <Menu.ItemGroup key="buttons" title={renderTitle(tType('button'))}>
        {buttons.map(({ icon, type, disabled }) => (
          <Menu.Item key={type} icon={icon} disabled={disabled}>
            {tWt(type)}
          </Menu.Item>
        ))}
      </Menu.ItemGroup>
    </Menu>
  );
  return (
    <Dropdown
      overlay={controlerItems}
      placement="bottomLeft"
      trigger={['click']}
    >
      <Tooltip title={t('controller')}>
        <ToolbarButton icon={<ControlOutlined />} />
      </Tooltip>
    </Dropdown>
  );
};
