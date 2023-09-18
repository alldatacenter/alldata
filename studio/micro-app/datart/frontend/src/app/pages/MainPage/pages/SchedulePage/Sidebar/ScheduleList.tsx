import {
  CaretRightOutlined,
  DeleteOutlined,
  EditOutlined,
  LoadingOutlined,
  MoreOutlined,
  PauseOutlined,
  SendOutlined,
} from '@ant-design/icons';
import { Menu, message, Popconfirm, TreeDataNode } from 'antd';
import { MenuListItem, Popup, Tree, TreeTitle } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { CommonFormTypes } from 'globalConstants';
import { FC, memo, useCallback, useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useHistory } from 'react-router-dom';
import { onDropTreeFn, stopPPG } from 'utils/utils';
import { CascadeAccess, getCascadeAccess } from '../../../Access';
import {
  selectIsOrgOwner,
  selectPermissionMap,
} from '../../../slice/selectors';
import {
  PermissionLevels,
  ResourceTypes,
} from '../../PermissionPage/constants';
import { useToScheduleDetails } from '../hooks';
import { SaveFormContext } from '../SaveFormContext';
import { executeSchedule, startSchedule, stopSchedule } from '../services';
import { useScheduleSlice } from '../slice';
import {
  selectDeleteLoading,
  selectEditingSchedule,
  selectScheduleListLoading,
} from '../slice/selectors';
import {
  deleteSchedule,
  getSchedules,
  updateScheduleBase,
} from '../slice/thunks';

export const ScheduleList: FC<{
  orgId: string;
  scheduleId?: string;
  list?: TreeDataNode[];
}> = memo(({ orgId, scheduleId, list }) => {
  const dispatch = useDispatch();
  const editingSchedule = useSelector(selectEditingSchedule);
  const loading = useSelector(selectScheduleListLoading);
  const deleteLoading = useSelector(selectDeleteLoading);
  const { toDetails } = useToScheduleDetails();
  const { actions } = useScheduleSlice();
  const [startLoading, setStartLoading] = useState(false);
  const [stopLoading, setStopLoading] = useState(false);
  const [executeLoading, setExecuteLoading] = useState(false);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);

  const onUpdateScheduleList = useCallback(() => {
    dispatch(getSchedules(orgId as string));
  }, [dispatch, orgId]);

  useEffect(() => {
    onUpdateScheduleList();
  }, [onUpdateScheduleList]);

  const history = useHistory();
  const { showSaveForm } = useContext(SaveFormContext);
  const isOwner = useSelector(selectIsOrgOwner);
  const permissionMap = useSelector(selectPermissionMap);
  const t = useI18NPrefix('schedule.sidebar.scheduleList');
  const tg = useI18NPrefix('global');

  const moreMenuClick = useCallback(
    ({ id, name, parentId, index }) =>
      ({ key, domEvent }) => {
        domEvent.stopPropagation();
        switch (key) {
          case 'info':
            showSaveForm({
              scheduleType: 'folder',
              type: CommonFormTypes.Edit,
              visible: true,
              simple: false,
              initialValues: {
                id,
                name,
                parentId,
              },
              parentIdLabel: t('parent'),
              onSave: (values, onClose) => {
                dispatch(
                  updateScheduleBase({
                    schedule: {
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
          case 'start':
            if (!startLoading) {
              setStartLoading(true);
              startSchedule(id)
                .then(res => {
                  if (!!res) {
                    message.success(t('successStarted'));
                    onUpdateScheduleList();
                    if (editingSchedule?.id === id) {
                      dispatch(actions.setEditingScheduleActive(true));
                    }
                  }
                })
                .finally(() => {
                  setStartLoading(false);
                });
            }
            break;
          case 'stop':
            if (!stopLoading) {
              setStopLoading(true);
              stopSchedule(id)
                .then(res => {
                  if (!!res) {
                    message.success(t('successStop'));
                    onUpdateScheduleList();
                    if (editingSchedule?.id === id) {
                      dispatch(actions.setEditingScheduleActive(false));
                    }
                  }
                })
                .finally(() => {
                  setStopLoading(false);
                });
            }
            break;
          case 'execute':
            if (!executeLoading) {
              setExecuteLoading(true);
              executeSchedule(id)
                .then(res => {
                  if (!!res) {
                    message.success(t('successImmediately'));
                  }
                })
                .finally(() => {
                  setExecuteLoading(false);
                });
            }
            break;
          case 'delete':
            break;
          default:
            break;
        }
      },
    [
      actions,
      dispatch,
      editingSchedule?.id,
      executeLoading,
      onUpdateScheduleList,
      orgId,
      showSaveForm,
      startLoading,
      stopLoading,
      t,
      toDetails,
    ],
  );

  const del = useCallback(
    (id, isFolder) => () => {
      if (deleteLoading) return;
      dispatch(
        deleteSchedule({
          id,
          archive: !isFolder,
          resolve: () => {
            message.success(
              `${t('success')}${isFolder ? t('delete') : t('moveToTrash')}`,
            );
            toDetails(orgId);
          },
        }),
      );
    },
    [deleteLoading, dispatch, t, toDetails, orgId],
  );

  const renderTreeTitle = useCallback(
    node => {
      const { title, path, isFolder, id, active } = node;
      const isAuthorized = getCascadeAccess(
        isOwner,
        permissionMap,
        ResourceTypes.Schedule,
        path,
        PermissionLevels.Manage,
      );
      return (
        <TreeTitle>
          <h4>{`${title}`}</h4>
          <CascadeAccess
            module={ResourceTypes.Schedule}
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
                  {isAuthorized && !active && !isFolder && (
                    <MenuListItem
                      key="start"
                      prefix={
                        startLoading ? (
                          <LoadingOutlined className="icon" />
                        ) : (
                          <CaretRightOutlined className="icon" />
                        )
                      }
                    >
                      {t('start')}
                    </MenuListItem>
                  )}
                  {isAuthorized && active && !isFolder && (
                    <MenuListItem
                      key="stop"
                      prefix={
                        stopLoading ? (
                          <LoadingOutlined className="icon" />
                        ) : (
                          <PauseOutlined className="icon" />
                        )
                      }
                    >
                      {t('stop')}
                    </MenuListItem>
                  )}

                  {isAuthorized && !isFolder && (
                    <MenuListItem
                      key="execute"
                      prefix={
                        executeLoading ? (
                          <LoadingOutlined className="icon" />
                        ) : (
                          <SendOutlined className="icon" />
                        )
                      }
                    >
                      {t('executeImmediately')}
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
    [
      isOwner,
      permissionMap,
      moreMenuClick,
      tg,
      startLoading,
      t,
      stopLoading,
      executeLoading,
      deleteLoading,
      del,
    ],
  );

  const menuSelect = useCallback(
    (_, { node }) => {
      if (node.type === 'FOLDER') {
        if (expandedKeys?.includes(node.key)) {
          setExpandedKeys(expandedKeys.filter(k => k !== node.key));
        } else {
          setExpandedKeys([node.key].concat(expandedKeys));
        }
      } else {
        history.push(`/organizations/${orgId}/schedules/${node.id}`);
      }
    },
    [expandedKeys, history, orgId],
  );

  const onDrop = info => {
    onDropTreeFn({
      info,
      treeData: list,
      callback: (id, parentId, index) => {
        dispatch(
          updateScheduleBase({
            schedule: {
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
      {...(scheduleId && { selectedKeys: [scheduleId] })}
      draggable
    />
  );
});
