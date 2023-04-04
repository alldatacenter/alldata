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
  EditOutlined,
  LoadingOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Menu, message, Popconfirm, TreeDataNode } from 'antd';
import { MenuListItem, Popup, Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  selectIsOrgOwner,
  selectOrgId,
  selectPermissionMap,
} from 'app/pages/MainPage/slice/selectors';
import { CommonFormTypes } from 'globalConstants';
import { memo, useCallback, useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router';
import { onDropTreeFn, stopPPG, uuidv4 } from 'utils/utils';
import { CascadeAccess, getCascadeAccess } from '../../../Access';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../PermissionPage/constants';
import { UNPERSISTED_ID_PREFIX } from '../../ViewPage/constants';
import { SaveFormContext } from '../SaveFormContext';
import {
  selectDeleteSourceLoading,
  selectSourceListLoading,
} from '../slice/selectors';
import { deleteSource, getSources, updateSourceBase } from '../slice/thunks';

interface SourceListProps {
  sourceId?: string;
  list?: TreeDataNode[];
}

export const SourceList = memo(({ sourceId, list }: SourceListProps) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const orgId = useSelector(selectOrgId);
  const deleteLoading = useSelector(selectDeleteSourceLoading);
  const loading = useSelector(selectSourceListLoading);
  const t = useI18NPrefix('source');
  const tg = useI18NPrefix('global');
  const { showSaveForm } = useContext(SaveFormContext);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);

  useEffect(() => {
    dispatch(getSources(orgId));
  }, [dispatch, orgId]);

  const toDetails = useCallback(
    id => () => {
      history.push(`/organizations/${orgId}/sources/${id}`);
    },
    [history, orgId],
  );

  const moreMenuClick = useCallback(
    ({ id, name, parentId, index }) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              sourceType: 'folder',
              type: CommonFormTypes.Edit,
              visible: true,
              simple: false,
              initialValues: {
                id,
                name,
                parentId,
              },
              parentIdLabel: t('sidebar.parent'),
              onSave: (values, onClose) => {
                dispatch(
                  updateSourceBase({
                    source: {
                      id,
                      index,
                      name: values.name,
                      parentId: values.parentId,
                    },
                    resolve: () => {
                      toDetails(orgId);
                      onClose();
                    },
                  }),
                );
              },
            });
            break;
          case 'addNewView':
            history.push({
              pathname: `/organizations/${orgId}/views/${`${UNPERSISTED_ID_PREFIX}${uuidv4()}`}`,
              state: {
                sourcesId: id,
              },
            });
            break;
          case 'delete':
            break;
          default:
            break;
        }
      },
    [dispatch, history, orgId, showSaveForm, t, toDetails],
  );

  const del = useCallback(
    (id, isFolder) => () => {
      if (deleteLoading) return;
      dispatch(
        deleteSource({
          id,
          archive: !isFolder,
          resolve: () => {
            message.success(
              isFolder
                ? tg('operation.archiveSuccess')
                : tg('operation.deleteSuccess'),
            );
            history.replace(`/organizations/${orgId}/sources`);
          },
        }),
      );
    },
    [deleteLoading, dispatch, tg, history, orgId],
  );

  const renderTreeTitle = useCallback(
    node => {
      const { title, path, isFolder, id, type } = node;
      const isAuthorized = getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.Source,
        path,
        PermissionLevels.Manage,
      );

      return (
        <TreeTitle>
          <h4>{title}</h4>
          <CascadeAccess
            module={ResourceTypes.Source}
            path={path}
            level={PermissionLevels.Manage}
          >
            <Popup
              trigger={['click']}
              placement="bottom"
              content={
                <Menu
                  prefixCls="ant-dropdown-menu"
                  selectable={false}
                  onClick={moreMenuClick(node)}
                >
                  {isAuthorized && (
                    <MenuListItem
                      key="info"
                      prefix={<EditOutlined className="icon" />}
                    >
                      {tg('button.info')}
                    </MenuListItem>
                  )}

                  {isAuthorized && type !== 'FOLDER' && (
                    <MenuListItem
                      key="addNewView"
                      prefix={<EditOutlined className="icon" />}
                    >
                      {t('creatView')}
                    </MenuListItem>
                  )}

                  {isAuthorized && (
                    <MenuListItem
                      key="delete"
                      prefix={
                        deleteLoading ? (
                          <LoadingOutlined className="icon" />
                        ) : (
                          <DeleteOutlined className="icon" />
                        )
                      }
                    >
                      <Popconfirm
                        title={
                          isFolder
                            ? tg('operation.deleteConfirm')
                            : tg('operation.archiveConfirm')
                        }
                        onConfirm={del(id, isFolder)}
                      >
                        {isFolder ? tg('button.delete') : tg('button.archive')}
                      </Popconfirm>
                    </MenuListItem>
                  )}
                </Menu>
              }
            >
              <span className="action" onClick={stopPPG}>
                <MoreOutlined />
              </span>
            </Popup>
          </CascadeAccess>
        </TreeTitle>
      );
    },
    [isOwner, permissionMap, moreMenuClick, tg, t, deleteLoading, del],
  );

  const onDrop = info => {
    onDropTreeFn({
      info,
      treeData: list,
      callback: (id, parentId, index) => {
        dispatch(
          updateSourceBase({
            source: {
              id,
              parentId,
              index: index,
              name: info.dragNode.name,
            },
            resolve: () => {},
          }),
        );
      },
    });
  };

  const menuSelect = useCallback(
    (_, { node }) => {
      if (node.type === 'FOLDER') {
        if (expandedKeys?.includes(node.key)) {
          setExpandedKeys(expandedKeys.filter(k => k !== node.key));
        } else {
          setExpandedKeys([node.key].concat(expandedKeys));
        }
      } else {
        history.push(`/organizations/${orgId}/sources/${node.id}`);
      }
    },
    [expandedKeys, history, orgId],
  );

  const handleExpandTreeNode = expandedKeys => {
    setExpandedKeys(expandedKeys);
  };

  return (
    <Tree
      loading={loading}
      treeData={list}
      titleRender={renderTreeTitle}
      onSelect={menuSelect}
      onDrop={onDrop}
      expandedKeys={expandedKeys}
      onExpand={handleExpandTreeNode}
      {...(sourceId && { selectedKeys: [sourceId] })}
      draggable
    />
  );
});
