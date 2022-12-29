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
  CalendarOutlined,
  DeleteOutlined,
  EditOutlined,
  FieldStringOutlined,
  FileUnknownOutlined,
  MoreOutlined,
  NumberOutlined,
} from '@ant-design/icons';
import { Menu, Popconfirm, Tooltip } from 'antd';
import { IW, MenuListItem, Popup } from 'app/components';
import { DataViewFieldType } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataViewMeta } from 'app/types/ChartDataViewMeta';
import { FC, memo } from 'react';
import styled from 'styled-components/macro';
import {
  FONT_SIZE_BASE,
  FONT_SIZE_TITLE,
  SPACE,
  SPACE_MD,
  SPACE_TIMES,
  SPACE_XS,
  WARNING,
} from 'styles/StyleConstants';

const DataModelComputerFieldNode: FC<{
  node: ChartDataViewMeta;
  menuClick: (node: ChartDataViewMeta, key: string) => void;
}> = memo(({ node, menuClick }) => {
  const t = useI18NPrefix('view.model');

  const renderNode = (node: ChartDataViewMeta) => {
    let icon;
    switch (node.type) {
      case DataViewFieldType.NUMERIC:
        icon = (
          <NumberOutlined style={{ alignSelf: 'center', color: WARNING }} />
        );
        break;
      case DataViewFieldType.STRING:
        icon = (
          <FieldStringOutlined
            style={{ alignSelf: 'center', color: WARNING }}
          />
        );
        break;
      case DataViewFieldType.DATE:
        icon = (
          <CalendarOutlined style={{ alignSelf: 'center', color: WARNING }} />
        );
        break;
      default:
        icon = (
          <FileUnknownOutlined
            style={{ alignSelf: 'center', color: WARNING }}
          />
        );
        break;
    }

    return (
      <>
        <div className="content">
          <Tooltip title={t('createComputedFields')} placement="left">
            <StyledIW fontSize={FONT_SIZE_TITLE}>{icon}</StyledIW>
          </Tooltip>
          <span>{node.name}</span>
        </div>
        <div className="action">
          <Popup
            trigger={['click']}
            placement="bottom"
            content={
              <Menu
                prefixCls="ant-dropdown-menu"
                selectable={false}
                onClick={({ key }) => menuClick(node, key)}
              >
                <MenuListItem
                  key="exit"
                  prefix={<EditOutlined className="icon" />}
                >
                  {t('edit')}
                </MenuListItem>
                <MenuListItem
                  key="del"
                  prefix={<DeleteOutlined className="icon" />}
                >
                  <Popconfirm
                    title={t('deleteSure')}
                    onConfirm={() => menuClick(node, 'delete')}
                  >
                    {t('delete')}
                  </Popconfirm>
                </MenuListItem>
              </Menu>
            }
          >
            <MoreOutlined />
          </Popup>
        </div>
      </>
    );
  };

  return (
    <StyledDataModelComputerFieldNode>
      {renderNode(node)}
    </StyledDataModelComputerFieldNode>
  );
});

export default DataModelComputerFieldNode;

const StyledDataModelComputerFieldNode = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: ${SPACE} ${SPACE_MD};
  font-size: ${FONT_SIZE_BASE};
  line-height: 32px;
  user-select: 'none';
  background: 'transparent';
  &.in-hierarchy {
    margin-right: 0;
  }

  & .content {
    display: flex;
    align-items: center;
  }

  & .action {
    display: none;
    padding-right: ${SPACE_XS};
  }
  &:hover {
    .action {
      display: inline-block;
    }
  }
`;

const StyledIW = styled(IW)`
  width: ${SPACE_TIMES(7)};
  height: ${SPACE_TIMES(7)};
  margin-right: ${SPACE_XS};
  cursor: pointer;
  border: 1px solid ${p => p.theme.borderColorSplit};
  border-radius: 4px;
`;
