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
  CloseOutlined,
  InfoCircleOutlined,
  LoadingOutlined,
} from '@ant-design/icons';
import { Button, Dropdown, Menu, Space } from 'antd';
import { Confirm, TabPane, Tabs as TabsComponent } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { selectOrgId } from 'app/pages/MainPage/slice/selectors';
import { memo, useCallback, useContext, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import styled, { css } from 'styled-components/macro';
import { LEVEL_1, ORANGE } from 'styles/StyleConstants';
import { ViewViewModelStages } from '../constants';
import { EditorContext } from '../EditorContext';
import {
  selectCurrentEditingViewAttr,
  selectEditingViews,
} from '../slice/selectors';
import {
  closeAllEditingViews,
  closeOtherEditingViews,
  removeEditingView,
  runSql,
} from '../slice/thunks';
import { ViewViewModel } from '../slice/types';

const errorColor = css`
  color: ${p => p.theme.error};
`;

export const Tabs = memo(() => {
  const [operatingView, setOperatingView] = useState<null | ViewViewModel>(
    null,
  );
  const [confirmVisible, setConfirmVisible] = useState(false);
  const dispatch = useDispatch();
  const history = useHistory();
  const { editorInstance } = useContext(EditorContext);
  const orgId = useSelector(selectOrgId);
  const editingViews = useSelector(selectEditingViews);
  const id = useSelector(state =>
    selectCurrentEditingViewAttr(state, { name: 'id' }),
  ) as string;

  const t = useI18NPrefix('view.tabs');

  const redirect = useCallback(
    (currentEditingViewKey?: string) => {
      if (currentEditingViewKey) {
        history.push(`/organizations/${orgId}/views/${currentEditingViewKey}`);
      } else {
        history.push(`/organizations/${orgId}/views`);
      }
    },
    [history, orgId],
  );

  const tabChange = useCallback(
    activeKey => {
      if (id !== activeKey) {
        history.push(`/organizations/${orgId}/views/${activeKey}`);
      }
    },
    [history, id, orgId],
  );

  const tabEdit = useCallback(
    (targetKey, action) => {
      const view = editingViews.find(v => v.id === targetKey);

      switch (action) {
        case 'remove':
          if (view!.touched === false) {
            dispatch(removeEditingView({ id: targetKey, resolve: redirect }));
          } else {
            setOperatingView(view!);
            setConfirmVisible(true);
          }
          break;
        default:
          break;
      }
    },
    [dispatch, editingViews, redirect],
  );

  const hideConfirm = useCallback(() => {
    setConfirmVisible(false);
  }, []);

  const removeTab = useCallback(() => {
    dispatch(removeEditingView({ id: operatingView!.id, resolve: redirect }));
    setConfirmVisible(false);
  }, [dispatch, operatingView, redirect]);

  const runTab = useCallback(() => {
    const fragment = editorInstance
      ?.getModel()
      ?.getValueInRange(editorInstance.getSelection()!);
    setConfirmVisible(false);
    dispatch(runSql({ id, isFragment: !!fragment }));
  }, [dispatch, id, editorInstance]);

  const handleClickMenu = (e: any, id: string) => {
    e.domEvent.stopPropagation();
    if (e.key === 'CLOSE_OTHER') {
      dispatch(closeOtherEditingViews({ id, resolve: redirect }));
      return;
    }
    dispatch(closeAllEditingViews({ resolve: redirect }));
  };

  const menu = (id: string) => (
    <Menu onClick={e => handleClickMenu(e, id)}>
      <Menu.Item key="CLOSE_OTHER">
        <span>{t('closeOther')}</span>
      </Menu.Item>
      <Menu.Item key="CLOSE_ALL">
        <span>{t('closeAll')}</span>
      </Menu.Item>
    </Menu>
  );

  const Tab = (id: string, name: string) => (
    <span>
      <Dropdown overlay={menu(id)} trigger={['contextMenu']}>
        <span className="ant-dropdown-link">{name}</span>
      </Dropdown>
    </span>
  );

  return (
    <Wrapper>
      <TabsComponent
        hideAdd
        type="editable-card"
        activeKey={id}
        onChange={tabChange}
        onEdit={tabEdit}
      >
        {editingViews.map(({ id, name, touched, stage, error }) => (
          <TabPane
            key={id}
            tab={error ? <span css={errorColor}>{name}</span> : Tab(id, name)}
            closeIcon={
              <CloseIcon touched={touched} stage={stage} error={!!error} />
            }
          />
        ))}
      </TabsComponent>
      <Confirm
        visible={confirmVisible}
        title={t('warning')}
        icon={<InfoCircleOutlined style={{ color: ORANGE }} />}
        footer={
          <Space>
            <Button onClick={removeTab}>{t('discard')}</Button>
            <Button onClick={hideConfirm}>{t('cancel')}</Button>
            <Button onClick={runTab} type="primary">
              {t('execute')}
            </Button>
          </Space>
        }
      />
    </Wrapper>
  );
});

interface CloseIconProps {
  touched: boolean;
  stage: ViewViewModelStages;
  error: boolean;
}

function CloseIcon({ touched, stage, error }: CloseIconProps) {
  const [hovering, setHovering] = useState(false);

  const onEnter = useCallback(() => {
    setHovering(true);
  }, []);

  const onLeave = useCallback(() => {
    setHovering(false);
  }, []);

  let icon;

  switch (stage) {
    case ViewViewModelStages.Loading:
    case ViewViewModelStages.Running:
    case ViewViewModelStages.Saving:
      icon = <LoadingOutlined />;
      break;
    default:
      if (!hovering) {
        if (error) {
          icon = <InfoCircleOutlined css={errorColor} />;
        } else if (touched) {
          icon = <Editing />;
        } else {
          icon = <CloseOutlined />;
        }
      } else {
        icon = <CloseOutlined />;
      }

      break;
  }

  return (
    <CloseIconWrapper onMouseEnter={onEnter} onMouseLeave={onLeave}>
      {icon}
    </CloseIconWrapper>
  );
}

const Wrapper = styled.div`
  z-index: ${LEVEL_1};
  flex-shrink: 0;
`;

const CloseIconWrapper = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 12px;
  height: 12px;
`;

const Editing = styled.span`
  display: block;
  width: 10px;
  height: 10px;
  background-color: ${p => p.theme.textColorLight};
  border-radius: 50%;
`;
