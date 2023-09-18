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
  DeleteOutlined,
  MoreOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { Menu, message, Popconfirm, TreeDataNode } from 'antd';
import { MenuListItem, Popup, Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectIsOrgOwner,
  selectOrgId,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import { getInsertedNodeIndex, stopPPG } from 'utils/utils';
import { SaveFormContext } from '../SaveFormContext';
import {
  selectArchivedListLoading,
  selectCurrentEditingViewKey,
  selectViews,
} from '../slice/selectors';
import {
  deleteView,
  getArchivedViews,
  removeEditingView,
  unarchiveView,
} from '../slice/thunks';

interface RecycleProps {
  list?: TreeDataNode[];
}

export const Recycle = memo(({ list }: RecycleProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const { showSaveForm } = useContext(SaveFormContext);
  const loading = useSelector(selectArchivedListLoading);
  const orgId = useSelector(selectOrgId);
  const currentEditingViewKey = useSelector(selectCurrentEditingViewKey);
  const views = useSelector(selectViews);
  const isOwner = useSelector(selectIsOrgOwner);
  const t = useI18NPrefix('view.saveForm');
  const tg = useI18NPrefix('global');

  useEffect(() => {
    dispatch(getArchivedViews(orgId));
  }, [dispatch, orgId]);

  const redirect = useCallback(
    currentEditingViewKey => {
      if (currentEditingViewKey) {
        history.push(`/organizations/${orgId}/views/${currentEditingViewKey}`);
      } else {
        history.push(`/organizations/${orgId}/views`);
      }
    },
    [history, orgId],
  );

  const del = useCallback(
    id => e => {
      e.stopPropagation();
      dispatch(
        deleteView({
          id,
          archive: false,
          resolve: () => {
            dispatch(removeEditingView({ id, resolve: redirect }));
            message.success(tg('operation.deleteSuccess'));
          },
        }),
      );
    },
    [dispatch, redirect, tg],
  );

  const moreMenuClick = useCallback(
    (id, name) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'reset':
            showSaveForm({
              type: CommonFormTypes.Edit,
              visible: true,
              simple: true,
              initialValues: { name, parentId: null },
              parentIdLabel: t('folder'),
              onSave: (values, onClose) => {
                let index = getInsertedNodeIndex(values, views);

                dispatch(
                  unarchiveView({
                    view: { ...values, id, index },
                    resolve: () => {
                      dispatch(removeEditingView({ id, resolve: redirect }));
                      message.success(tg('operation.restoreSuccess'));
                      onClose();
                    },
                  }),
                );
              },
            });
            break;
          default:
            break;
        }
      },
    [dispatch, showSaveForm, redirect, views, t, tg],
  );

  const renderTreeTitle = useCallback(
    ({ key, title }) => {
      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          {isOwner && (
            <Popup
              trigger={['click']}
              placement="bottomRight"
              content={
                <Menu
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick(key, title)}
                >
                  <MenuListItem
                    key="reset"
                    prefix={<ReloadOutlined className="icon" />}
                  >
                    {tg('button.restore')}
                  </MenuListItem>
                  <MenuListItem
                    key="delelte"
                    prefix={<DeleteOutlined className="icon" />}
                  >
                    <Popconfirm
                      title={tg('operation.deleteConfirm')}
                      onConfirm={del(key)}
                    >
                      {tg('button.delete')}
                    </Popconfirm>
                  </MenuListItem>
                </Menu>
              }
            >
              <span className="action" onClick={stopPPG}>
                <MoreOutlined />
              </span>
            </Popup>
          )}
        </TreeTitle>
      );
    },
    [moreMenuClick, del, isOwner, tg],
  );

  const treeSelect = useCallback(
    (_, { node }) => {
      if (!node.isFolder && node.key !== currentEditingViewKey) {
        history.push(`/organizations/${orgId}/views/${node.key}`);
      }
    },
    [history, orgId, currentEditingViewKey],
  );

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={renderTreeTitle}
      onSelect={treeSelect}
    />
  );
});
